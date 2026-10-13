package io.github.localsoul.runtime.system;

/**
 * {@code System.Math} mit .NET-Semantik. Alle Funktionen über {@link StrictMath} (deterministisch).
 * Unterschiede zu Java: {@code Round} rundet zur geraden Zahl (Banker's Rounding), {@code
 * Abs(int.MinValue)} wirft {@link OverflowException}, {@code Clamp} mit {@code min > max} wirft
 * {@link ArgumentException}.
 */
public final class Math {

  private Math() {}

  /**
   * {@code Math.PI}.
   *
   * @return π
   */
  public static double PI() {
    return java.lang.Math.PI;
  }

  /**
   * {@code Math.E}.
   *
   * @return e
   */
  public static double E() {
    return java.lang.Math.E;
  }

  /**
   * {@code Math.Abs(int)}.
   *
   * @param value Wert
   * @return Betrag
   * @throws OverflowException für {@code int.MinValue}
   */
  public static int abs(int value) {
    if (value == Integer.MIN_VALUE) {
      throw new OverflowException(
          "Negating the minimum value of a twos complement number is invalid.");
    }
    return value < 0 ? -value : value;
  }

  /**
   * {@code Math.Abs(float)}.
   *
   * @param value Wert
   * @return Betrag
   */
  public static float abs(float value) {
    return java.lang.Math.abs(value);
  }

  /**
   * {@code Math.Abs(double)}.
   *
   * @param value Wert
   * @return Betrag
   */
  public static double abs(double value) {
    return java.lang.Math.abs(value);
  }

  /**
   * {@code Math.Max(int, int)}.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Maximum
   */
  public static int max(int a, int b) {
    return java.lang.Math.max(a, b);
  }

  /**
   * {@code Math.Max(float, float)}; NaN gewinnt wie in .NET.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Maximum
   */
  public static float max(float a, float b) {
    return java.lang.Math.max(a, b);
  }

  /**
   * {@code Math.Max(double, double)}; NaN gewinnt wie in .NET.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Maximum
   */
  public static double max(double a, double b) {
    return java.lang.Math.max(a, b);
  }

  /**
   * {@code Math.Min(int, int)}.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Minimum
   */
  public static int min(int a, int b) {
    return java.lang.Math.min(a, b);
  }

  /**
   * {@code Math.Min(float, float)}.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Minimum
   */
  public static float min(float a, float b) {
    return java.lang.Math.min(a, b);
  }

  /**
   * {@code Math.Min(double, double)}.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Minimum
   */
  public static double min(double a, double b) {
    return java.lang.Math.min(a, b);
  }

  /**
   * {@code Math.Clamp(int, int, int)}.
   *
   * @param value Wert
   * @param min untere Grenze
   * @param max obere Grenze
   * @return begrenzter Wert
   * @throws ArgumentException wenn {@code min > max}
   */
  public static int clamp(int value, int min, int max) {
    if (min > max) {
      throw new ArgumentException("'" + min + "' cannot be greater than " + max + ".");
    }
    return value < min ? min : (value > max ? max : value);
  }

  /**
   * {@code Math.Clamp(float, float, float)}.
   *
   * @param value Wert
   * @param min untere Grenze
   * @param max obere Grenze
   * @return begrenzter Wert
   * @throws ArgumentException wenn {@code min > max}
   */
  public static float clamp(float value, float min, float max) {
    if (min > max) {
      throw new ArgumentException(
          "'" + Convert.toString(min) + "' cannot be greater than " + Convert.toString(max) + ".");
    }
    return value < min ? min : (value > max ? max : value);
  }

  /**
   * {@code Math.Sign(int)}.
   *
   * @param value Wert
   * @return −1, 0 oder 1
   */
  public static int sign(int value) {
    return Integer.signum(value);
  }

  /**
   * {@code Math.Sqrt}.
   *
   * @param value Wert
   * @return Quadratwurzel
   */
  public static double sqrt(double value) {
    return StrictMath.sqrt(value);
  }

  /**
   * {@code Math.Pow}.
   *
   * @param x Basis
   * @param y Exponent
   * @return Potenz
   */
  public static double pow(double x, double y) {
    return StrictMath.pow(x, y);
  }

  /**
   * {@code Math.Floor}.
   *
   * @param value Wert
   * @return abgerundet
   */
  public static double floor(double value) {
    return StrictMath.floor(value);
  }

  /**
   * {@code Math.Ceiling}.
   *
   * @param value Wert
   * @return aufgerundet
   */
  public static double ceiling(double value) {
    return StrictMath.ceil(value);
  }

  /**
   * {@code Math.Round(double)}: zur nächsten geraden Zahl bei .5 (Banker's Rounding).
   *
   * @param value Wert
   * @return gerundet
   */
  public static double round(double value) {
    return StrictMath.rint(value);
  }

  /**
   * {@code Math.Sin}.
   *
   * @param radians Winkel
   * @return Sinus
   */
  public static double sin(double radians) {
    return StrictMath.sin(radians);
  }

  /**
   * {@code Math.Cos}.
   *
   * @param radians Winkel
   * @return Kosinus
   */
  public static double cos(double radians) {
    return StrictMath.cos(radians);
  }

  /**
   * {@code Math.Tan}.
   *
   * @param radians Winkel
   * @return Tangens
   */
  public static double tan(double radians) {
    return StrictMath.tan(radians);
  }

  /**
   * {@code Math.Atan2}.
   *
   * @param y Y
   * @param x X
   * @return Winkel
   */
  public static double atan2(double y, double x) {
    return StrictMath.atan2(y, x);
  }
}
