package io.github.localsoul.runtime.system;

/**
 * {@code System.MathF}: {@code float}-Varianten, intern über {@link StrictMath} in {@code double}
 * berechnet und gerundet (deterministisch). {@code Round} mit Banker's Rounding.
 */
public final class MathF {

  private MathF() {}

  /**
   * {@code MathF.PI}.
   *
   * @return π als {@code float}
   */
  public static float PI() {
    return (float) java.lang.Math.PI;
  }

  /**
   * {@code MathF.E}.
   *
   * @return e als {@code float}
   */
  public static float E() {
    return (float) java.lang.Math.E;
  }

  /**
   * {@code MathF.Abs}.
   *
   * @param x Wert
   * @return Betrag
   */
  public static float abs(float x) {
    return java.lang.Math.abs(x);
  }

  /**
   * {@code MathF.Sqrt}.
   *
   * @param x Wert
   * @return Wurzel
   */
  public static float sqrt(float x) {
    return (float) StrictMath.sqrt(x);
  }

  /**
   * {@code MathF.Sin}.
   *
   * @param x Winkel
   * @return Sinus
   */
  public static float sin(float x) {
    return (float) StrictMath.sin(x);
  }

  /**
   * {@code MathF.Cos}.
   *
   * @param x Winkel
   * @return Kosinus
   */
  public static float cos(float x) {
    return (float) StrictMath.cos(x);
  }

  /**
   * {@code MathF.Atan2}.
   *
   * @param y Y
   * @param x X
   * @return Winkel
   */
  public static float atan2(float y, float x) {
    return (float) StrictMath.atan2(y, x);
  }

  /**
   * {@code MathF.Pow}.
   *
   * @param x Basis
   * @param y Exponent
   * @return Potenz
   */
  public static float pow(float x, float y) {
    return (float) StrictMath.pow(x, y);
  }

  /**
   * {@code MathF.Floor}.
   *
   * @param x Wert
   * @return abgerundet
   */
  public static float floor(float x) {
    return (float) StrictMath.floor(x);
  }

  /**
   * {@code MathF.Ceiling}.
   *
   * @param x Wert
   * @return aufgerundet
   */
  public static float ceiling(float x) {
    return (float) StrictMath.ceil(x);
  }

  /**
   * {@code MathF.Round}: Banker's Rounding.
   *
   * @param x Wert
   * @return gerundet
   */
  public static float round(float x) {
    return (float) StrictMath.rint(x);
  }

  /**
   * {@code MathF.Max}.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Maximum
   */
  public static float max(float a, float b) {
    return java.lang.Math.max(a, b);
  }

  /**
   * {@code MathF.Min}.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Minimum
   */
  public static float min(float a, float b) {
    return java.lang.Math.min(a, b);
  }
}
