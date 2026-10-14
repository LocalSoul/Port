package io.github.localsoul.launcher.boot;

import io.github.localsoul.port.engine.api.boot.EngineBinding;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystems.input.InputScript;
import io.github.localsoul.port.engine.core.subsystems.input.ScriptedInput;
import java.io.PrintStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ServiceLoader;

/**
 * Startklasse der Spiel-JVM für {@code port launch run}: baut eine {@link CoreEngine}, bindet den
 * Adapter über {@link EngineBinding} ({@link ServiceLoader}) an, legt je Spielklasse ein Objekt an,
 * läuft N Frames und gibt den Welt-Dump auf stdout aus.
 *
 * <p>Aufruf: {@code --adapter <name> [--frames n] [--fixed-delta s] [--frame-delta s] [--axis
 * Name=Wert]... [--] Klasse...}. Exit-Codes: 0 Erfolg, 1 Fehler im Spiel, 2 ungültige Argumente
 * oder kein passender Adapter, 3 Spielklasse nicht gefunden.
 *
 * <p>Die Spielklassen werden per Namen geladen und instanziiert; das ist die einzige Reflection der
 * Plattform (Übergang bis zum Szenenformat, ADR-010).
 */
public final class BootMain {

  static final int OK = 0;
  static final int FAILED = 1;
  static final int USAGE = 2;
  static final int NOT_FOUND = 3;

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
    InputScript input = new InputScript().at(0);
    for (BootOptions.Axis axis : options.axes()) {
      input.axis(axis.name(), axis.value());
    }
    CoreEngine engine =
        CoreEngine.builder()
            .fixedDelta(options.fixedDelta())
            .frameDelta(options.frameDelta())
            .subsystem(new ScriptedInput(input))
            .build();
    binding.install(engine);
    try {
      for (String className : options.classes()) {
        Object script = instantiate(className, loader);
        binding.spawn(simpleName(className), script);
      }
      engine.step(options.frames());
    } catch (ClassNotFoundException e) {
      err.println("launcher-boot: Spielklasse nicht gefunden: " + e.getMessage());
      return NOT_FOUND;
    } catch (ReflectiveOperationException | RuntimeException e) {
      err.println("launcher-boot: Fehler im Spiel: " + describe(e));
      return FAILED;
    }
    out.println("frames: " + engine.frameCount());
    out.print(engine.world().dump());
    out.flush();
    engine.shutdown();
    return OK;
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
