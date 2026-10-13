package io.github.localsoul.port.engine.api.math;

/** Gemeinsame Konstanten und Hilfen für Gleitkommavergleiche. */
public final class MathConstants {

  /**
   * Standardtoleranz für {@code approxEquals}: absolute Abweichung je Komponente. Passend für Werte
   * in der Größenordnung 1 (Meter, Einheitsvektoren, Quaternionen).
   */
  public static final float EPSILON = 1e-5f;

  private MathConstants() {}

  /**
   * Vergleicht zwei Zahlen mit absoluter Toleranz.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @param epsilon erlaubte Abweichung, nicht negativ
   * @return {@code true}, wenn {@code |a - b| <= epsilon}
   */
  public static boolean approxEquals(float a, float b, float epsilon) {
    return Math.abs(a - b) <= epsilon;
  }

  /**
   * Quadratwurzel in {@code float}, deterministisch.
   *
   * @param value Wert, nicht negativ
   * @return Quadratwurzel
   */
  static float sqrt(float value) {
    return (float) StrictMath.sqrt(value);
  }
}
