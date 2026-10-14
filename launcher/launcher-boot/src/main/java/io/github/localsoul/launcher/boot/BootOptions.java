package io.github.localsoul.launcher.boot;

import java.util.ArrayList;
import java.util.List;

/**
 * Argumente von {@link BootMain}.
 *
 * @param adapter Name des Adapters
 * @param frames Anzahl der Frames ({@code 0} mit {@code --realtime}: bis zum Beenden)
 * @param fixedDelta Fixed-Timestep in Sekunden
 * @param frameDelta Frame-Dauer in Sekunden (mit {@code --realtime}: Zielwert der Schleife)
 * @param axes Achsenwerte ab Frame 0
 * @param keys Tastenskript für {@code null}/{@code offscreen}
 * @param render Ausgabe: {@code null}, {@code offscreen} oder {@code window}
 * @param width Breite der Ausgabe in Pixeln
 * @param height Höhe der Ausgabe in Pixeln
 * @param realtime Frames mit gemessener Dauer bis zum Beenden oder Fenster-Schließen
 * @param seed Startwert für Zufallszahlen des Adapters oder {@code null}
 * @param screenshots Bilder nach bestimmten Frames (nur {@code offscreen})
 * @param classes voll qualifizierte Spielklassen
 */
record BootOptions(
    String adapter,
    int frames,
    float fixedDelta,
    float frameDelta,
    List<Axis> axes,
    List<Key> keys,
    String render,
    int width,
    int height,
    boolean realtime,
    String seed,
    List<Screenshot> screenshots,
    List<String> classes) {

  /**
   * Achsenwert.
   *
   * @param name Achse
   * @param value Wert
   */
  record Axis(String name, float value) {}

  /**
   * Tastenereignis des Skripts.
   *
   * @param frame Frame, in dem es wirkt
   * @param key neutraler Tastenname (z. B. {@code LEFT}, {@code R})
   * @param down gedrückt ({@code --press}) oder losgelassen ({@code --release})
   */
  record Key(long frame, String key, boolean down) {}

  /**
   * Bild nach einem Frame.
   *
   * @param frame Anzahl der fertigen Frames, nach denen das Bild entsteht
   * @param file PNG-Datei
   */
  record Screenshot(long frame, String file) {}

  static final int DEFAULT_FRAMES = 60;
  static final float DEFAULT_FIXED_DELTA = 0.02f;
  static final float DEFAULT_FRAME_DELTA = 1f / 60f;
  static final String RENDER_NULL = "null";
  static final String RENDER_OFFSCREEN = "offscreen";
  static final String RENDER_WINDOW = "window";
  static final int DEFAULT_WIDTH = 960;
  static final int DEFAULT_HEIGHT = 600;

  BootOptions {
    axes = List.copyOf(axes);
    keys = List.copyOf(keys);
    screenshots = List.copyOf(screenshots);
    classes = List.copyOf(classes);
  }

  /**
   * Liest die Kommandozeile.
   *
   * @throws IllegalArgumentException bei unbekannter Option, fehlendem oder ungültigem Wert, ohne
   *     Adapter oder bei unverträglichen Optionen
   */
  static BootOptions parse(String[] args) {
    String adapter = null;
    int frames = DEFAULT_FRAMES;
    float fixedDelta = DEFAULT_FIXED_DELTA;
    float frameDelta = DEFAULT_FRAME_DELTA;
    List<Axis> axes = new ArrayList<>();
    List<Key> keys = new ArrayList<>();
    String render = RENDER_NULL;
    int width = DEFAULT_WIDTH;
    int height = DEFAULT_HEIGHT;
    boolean realtime = false;
    String seed = null;
    List<Screenshot> screenshots = new ArrayList<>();
    List<String> classes = new ArrayList<>();
    boolean onlyClasses = false;
    for (int i = 0; i < args.length; i++) {
      String arg = args[i];
      if (onlyClasses || !arg.startsWith("--")) {
        classes.add(arg);
        continue;
      }
      switch (arg) {
        case "--" -> onlyClasses = true;
        case "--adapter" -> adapter = value(args, ++i, arg);
        case "--frames" -> frames = (int) number(value(args, ++i, arg), arg);
        case "--fixed-delta" -> fixedDelta = (float) number(value(args, ++i, arg), arg);
        case "--frame-delta" -> frameDelta = (float) number(value(args, ++i, arg), arg);
        case "--axis" -> axes.add(axis(value(args, ++i, arg)));
        case "--press" -> keys.add(key(value(args, ++i, arg), true, arg));
        case "--release" -> keys.add(key(value(args, ++i, arg), false, arg));
        case "--render" -> render = render(value(args, ++i, arg));
        case "--size" -> {
          int[] size = size(value(args, ++i, arg));
          width = size[0];
          height = size[1];
        }
        case "--realtime" -> realtime = true;
        case "--seed" -> seed = Long.toString((long) number(value(args, ++i, arg), arg));
        case "--screenshot" -> screenshots.add(screenshot(value(args, ++i, arg)));
        default -> throw new IllegalArgumentException("unbekannte Option " + arg);
      }
    }
    if (adapter == null) {
      throw new IllegalArgumentException("--adapter fehlt");
    }
    if (frames < 0) {
      throw new IllegalArgumentException("--frames muss >= 0 sein");
    }
    if (!screenshots.isEmpty() && !render.equals(RENDER_OFFSCREEN)) {
      throw new IllegalArgumentException("--screenshot braucht --render offscreen");
    }
    if (!keys.isEmpty() && render.equals(RENDER_WINDOW)) {
      throw new IllegalArgumentException("--press/--release gehen nicht mit --render window");
    }
    return new BootOptions(
        adapter,
        frames,
        fixedDelta,
        frameDelta,
        axes,
        keys,
        render,
        width,
        height,
        realtime,
        seed,
        screenshots,
        classes);
  }

  private static String value(String[] args, int index, String option) {
    if (index >= args.length) {
      throw new IllegalArgumentException(option + " ohne Wert");
    }
    return args[index];
  }

  private static double number(String text, String option) {
    try {
      return Double.parseDouble(text);
    } catch (NumberFormatException e) {
      throw new IllegalArgumentException(option + " erwartet eine Zahl: " + text);
    }
  }

  private static Axis axis(String text) {
    int equals = text.indexOf('=');
    if (equals <= 0) {
      throw new IllegalArgumentException("--axis erwartet Name=Wert: " + text);
    }
    return new Axis(
        text.substring(0, equals), (float) number(text.substring(equals + 1), "--axis"));
  }

  private static Key key(String text, boolean down, String option) {
    int colon = text.indexOf(':');
    if (colon <= 0 || colon == text.length() - 1) {
      throw new IllegalArgumentException(option + " erwartet Frame:Taste: " + text);
    }
    long frame = (long) number(text.substring(0, colon), option);
    if (frame < 0) {
      throw new IllegalArgumentException(option + ": Frame muss >= 0 sein: " + text);
    }
    return new Key(frame, text.substring(colon + 1), down);
  }

  private static String render(String text) {
    if (text.equals(RENDER_NULL) || text.equals(RENDER_OFFSCREEN) || text.equals(RENDER_WINDOW)) {
      return text;
    }
    throw new IllegalArgumentException("--render erwartet null, offscreen oder window: " + text);
  }

  private static int[] size(String text) {
    int x = text.indexOf('x');
    if (x <= 0) {
      throw new IllegalArgumentException("--size erwartet BreitexHöhe: " + text);
    }
    int width = (int) number(text.substring(0, x), "--size");
    int height = (int) number(text.substring(x + 1), "--size");
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("--size muss positiv sein: " + text);
    }
    return new int[] {width, height};
  }

  private static Screenshot screenshot(String text) {
    int colon = text.indexOf(':');
    if (colon <= 0 || colon == text.length() - 1) {
      throw new IllegalArgumentException("--screenshot erwartet Frame:Datei: " + text);
    }
    long frame = (long) number(text.substring(0, colon), "--screenshot");
    if (frame < 1) {
      throw new IllegalArgumentException("--screenshot: Frame muss >= 1 sein: " + text);
    }
    return new Screenshot(frame, text.substring(colon + 1));
  }
}
