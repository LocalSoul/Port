package io.github.localsoul.launcher.boot;

import java.util.ArrayList;
import java.util.List;

/**
 * Argumente von {@link BootMain}.
 *
 * @param adapter Name des Adapters
 * @param frames Anzahl der Frames
 * @param fixedDelta Fixed-Timestep in Sekunden
 * @param frameDelta Frame-Dauer in Sekunden
 * @param axes Achsenwerte ab Frame 0
 * @param classes voll qualifizierte Spielklassen
 */
record BootOptions(
    String adapter,
    int frames,
    float fixedDelta,
    float frameDelta,
    List<Axis> axes,
    List<String> classes) {

  /**
   * Achsenwert.
   *
   * @param name Achse
   * @param value Wert
   */
  record Axis(String name, float value) {}

  static final int DEFAULT_FRAMES = 60;
  static final float DEFAULT_FIXED_DELTA = 0.02f;
  static final float DEFAULT_FRAME_DELTA = 1f / 60f;

  BootOptions {
    axes = List.copyOf(axes);
    classes = List.copyOf(classes);
  }

  /**
   * Liest die Kommandozeile.
   *
   * @throws IllegalArgumentException bei unbekannter Option, fehlendem Wert oder ohne Adapter
   */
  static BootOptions parse(String[] args) {
    String adapter = null;
    int frames = DEFAULT_FRAMES;
    float fixedDelta = DEFAULT_FIXED_DELTA;
    float frameDelta = DEFAULT_FRAME_DELTA;
    List<Axis> axes = new ArrayList<>();
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
        default -> throw new IllegalArgumentException("unbekannte Option " + arg);
      }
    }
    if (adapter == null) {
      throw new IllegalArgumentException("--adapter fehlt");
    }
    if (frames < 0) {
      throw new IllegalArgumentException("--frames muss >= 0 sein");
    }
    return new BootOptions(adapter, frames, fixedDelta, frameDelta, axes, classes);
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
}
