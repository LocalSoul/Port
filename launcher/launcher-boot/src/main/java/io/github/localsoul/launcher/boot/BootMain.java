package io.github.localsoul.launcher.boot;

import io.github.localsoul.port.engine.api.boot.EngineBinding;
import io.github.localsoul.port.engine.api.render.RenderBackend;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystems.input.InputScript;
import io.github.localsoul.port.engine.core.subsystems.input.KeyboardInput;
import io.github.localsoul.port.engine.core.subsystems.input.ScriptedInput;
import io.github.localsoul.port.engine.core.subsystems.render.SpriteRenderSystem;
import io.github.localsoul.port.render.swing.OffscreenBackend;
import io.github.localsoul.port.render.swing.WindowBackend;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.ServiceLoader;

/**
 * Startklasse der Spiel-JVM für {@code port launch run}: baut eine {@link CoreEngine}, bindet den
 * Adapter über {@link EngineBinding} ({@link ServiceLoader}) an, legt je Spielklasse ein Objekt an,
 * läuft N Frames und gibt den Welt-Dump auf stdout aus.
 *
 * <p>Aufruf: {@code --adapter <name> [--frames n] [--fixed-delta s] [--frame-delta s] [--axis
 * Name=Wert]... [--press Frame:Taste]... [--release Frame:Taste]... [--render
 * null|offscreen|window] [--size BxH] [--realtime] [--seed n] [--screenshot Frame:Datei]... [--]
 * Klasse...}.
 *
 * <p>Ausgabe (ADR-014): {@code null} zeichnet nicht, {@code offscreen} zeichnet in ein Bild (für
 * Tests und {@code --screenshot}), {@code window} öffnet ein Fenster mit Tastatureingabe. Ohne
 * Fenster kommen Tasten und Achsen aus dem Skript ({@code --press}, {@code --release}, {@code
 * --axis}). Mit {@code --realtime} laufen Frames mit gemessener Dauer, bis das Spiel beendet, das
 * Fenster geschlossen oder {@code --frames} erreicht ist ({@code 0}: unbegrenzt); sonst laufen
 * genau {@code --frames} Frames mit fester Dauer (früher, wenn das Spiel beendet).
 *
 * <p>Exit-Codes: 0 Erfolg, 1 Fehler im Spiel oder bei der Ausgabe, 2 ungültige Argumente oder kein
 * passender Adapter, 3 Spielklasse nicht gefunden.
 *
 * <p>Die Spielklassen werden per Namen geladen und instanziiert; das ist die einzige Reflection der
 * Plattform (Übergang bis zum Szenenformat, ADR-010).
 */
public final class BootMain {

  static final int OK = 0;
  static final int FAILED = 1;
  static final int USAGE = 2;
  static final int NOT_FOUND = 3;

  /** Obergrenze der gemessenen Frame-Dauer (wie {@code Time.maximumDeltaTime} in Unity). */
  static final float MAX_DELTA = 1f / 3f;

  private BootMain() {}

  /**
   * Startet und beendet die JVM mit dem Exit-Code.
   *
   * @param args Kommandozeile
   */
  public static void main(String[] args) {
    int code = run(args, System.out, System.err, BootMain.class.getClassLoader());
    System.exit(code);
  }

  /**
   * Führt einen Start aus, ohne die JVM zu beenden (für Tests).
   *
   * @param args Kommandozeile
   * @param out stdout
   * @param err stderr
   * @param loader Klassenlader für Adapter und Spiel
   * @return Exit-Code
   */
  static int run(String[] args, PrintStream out, PrintStream err, ClassLoader loader) {
    BootOptions options;
    try {
      options = BootOptions.parse(args);
    } catch (IllegalArgumentException e) {
      err.println("launcher-boot: " + e.getMessage());
      return USAGE;
    }
    EngineBinding binding = binding(options.adapter(), loader);
    if (binding == null) {
      err.println("launcher-boot: kein EngineBinding für Adapter '" + options.adapter() + "'");
      return USAGE;
    }
    Map<String, String> settings = new LinkedHashMap<>();
    if (options.seed() != null) {
      settings.put("seed", options.seed());
    }
    CoreEngine.Builder builder =
        CoreEngine.builder().fixedDelta(options.fixedDelta()).frameDelta(options.frameDelta());
    OffscreenBackend offscreen = null;
    RenderBackend backend = null;
    try {
      if (options.render().equals(BootOptions.RENDER_WINDOW)) {
        backend = new WindowBackend(title(options), options.width(), options.height());
        builder.subsystem(new KeyboardInput());
      } else {
        builder.subsystem(new ScriptedInput(script(options)));
      }
      if (options.render().equals(BootOptions.RENDER_OFFSCREEN)) {
        offscreen = new OffscreenBackend(options.width(), options.height());
        backend = offscreen;
      }
    } catch (RuntimeException | java.awt.AWTError e) {
      err.println("launcher-boot: Ausgabe nicht verfügbar: " + e);
      return FAILED;
    }
    if (backend != null) {
      builder.subsystem(new SpriteRenderSystem(backend));
    }
    CoreEngine engine = builder.build();
    try {
      binding.configure(settings);
      binding.install(engine);
      for (String className : options.classes()) {
        Object script = instantiate(className, loader);
        binding.spawn(simpleName(className), script);
      }
      if (options.realtime()) {
        runRealtime(engine, options, backend);
      } else {
        runFixed(engine, options, offscreen, err);
      }
    } catch (ClassNotFoundException e) {
      err.println("launcher-boot: Spielklasse nicht gefunden: " + e.getMessage());
      engine.shutdown();
      return NOT_FOUND;
    } catch (IOException e) {
      err.println("launcher-boot: Bild nicht geschrieben: " + e.getMessage());
      engine.shutdown();
      return FAILED;
    } catch (ReflectiveOperationException | RuntimeException e) {
      err.println("launcher-boot: Fehler im Spiel: " + describe(e));
      engine.shutdown();
      return FAILED;
    }
    out.println("frames: " + engine.frameCount());
    if (!options.realtime()) {
      out.print(engine.world().dump());
    }
    out.flush();
    engine.shutdown();
    return OK;
  }

  /** Achsen ab Frame 0 und Tasten aus {@code --press}/{@code --release}. */
  private static InputScript script(BootOptions options) {
    InputScript input = new InputScript().at(0);
    for (BootOptions.Axis axis : options.axes()) {
      input.axis(axis.name(), axis.value());
    }
    for (BootOptions.Key key : options.keys()) {
      input.at(key.frame());
      if (key.down()) {
        input.press(key.key());
      } else {
        input.release(key.key());
      }
    }
    return input;
  }

  /** Feste Frame-Dauer; Bilder nach den gewünschten Frames. */
  private static void runFixed(
      CoreEngine engine, BootOptions options, OffscreenBackend offscreen, PrintStream err)
      throws IOException {
    for (int i = 0; i < options.frames() && !engine.quitRequested(); i++) {
      engine.step(1);
      for (BootOptions.Screenshot shot : options.screenshots()) {
        if (shot.frame() == engine.frameCount()) {
          offscreen.writePng(Path.of(shot.file()));
        }
      }
    }
    for (BootOptions.Screenshot shot : options.screenshots()) {
      if (shot.frame() > engine.frameCount()) {
        err.println(
            "launcher-boot: kein Bild nach Frame " + shot.frame() + " (Lauf endete früher)");
      }
    }
  }

  /**
   * Gemessene Frame-Dauer (höchstens {@link #MAX_DELTA}), Ziel {@code frameDelta} je Frame; endet
   * mit {@code Application.Quit}, geschlossenem Fenster oder nach {@code --frames} (0: nie).
   */
  private static void runRealtime(CoreEngine engine, BootOptions options, RenderBackend backend) {
    long targetNanos = (long) (options.frameDelta() * 1_000_000_000.0);
    float delta = options.frameDelta();
    while (!engine.quitRequested()
        && (backend == null || !backend.closed())
        && (options.frames() == 0 || engine.frameCount() < options.frames())) {
      long start = System.nanoTime();
      engine.stepFrame(delta);
      long busy = System.nanoTime() - start;
      if (busy < targetNanos) {
        try {
          Thread.sleep(
              (targetNanos - busy) / 1_000_000L, (int) ((targetNanos - busy) % 1_000_000L));
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
          return;
        }
      }
      delta = Math.min(MAX_DELTA, (System.nanoTime() - start) / 1_000_000_000f);
    }
  }

  private static String title(BootOptions options) {
    if (options.classes().isEmpty()) {
      return "Recomp";
    }
    return "Recomp – " + simpleName(options.classes().get(0));
  }

  private static EngineBinding binding(String adapter, ClassLoader loader) {
    for (EngineBinding binding : ServiceLoader.load(EngineBinding.class, loader)) {
      if (binding.adapter().equals(adapter)) {
        return binding;
      }
    }
    return null;
  }

  private static Object instantiate(String className, ClassLoader loader)
      throws ReflectiveOperationException {
    Class<?> type = Class.forName(className, true, loader);
    return type.getDeclaredConstructor().newInstance();
  }

  private static String simpleName(String className) {
    return className.substring(className.lastIndexOf('.') + 1);
  }

  private static String describe(Exception e) {
    Throwable cause = e instanceof InvocationTargetException target ? target.getCause() : e;
    return cause.getClass().getName() + ": " + cause.getMessage();
  }
}
