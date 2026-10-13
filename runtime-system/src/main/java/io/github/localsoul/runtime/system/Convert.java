package io.github.localsoul.runtime.system;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

/**
 * {@code ToString()} der .NET-Grundtypen im Format von .NET Framework / Unity-Mono (ADR-019),
 * invariant (Punkt als Dezimaltrenner).
 *
 * <ul>
 *   <li>{@code float}: „G7“, {@code double}: „G15“ – höchstens 7 bzw. 15 signifikante Stellen,
 *       kaufmännisch gerundet, ohne nachgestellte Nullen ({@code 5f} → „5“, {@code 1/3f} →
 *       „0.3333333“).
 *   <li>Exponentenform, wenn der Exponent mindestens der Stellenzahl entspricht oder kleiner als −4
 *       ist: {@code 1E-05}, {@code 1.234568E+08} (Vorzeichen immer, mindestens zwei Ziffern).
 *   <li>{@code -0.0} ergibt „0“ (.NET Framework), NaN „NaN“, Unendlich „Infinity“/„-Infinity“.
 *   <li>{@code bool}: „True“/„False“; {@code null} in Verkettungen: „“.
 * </ul>
 */
public final class Convert {

  private Convert() {}

  /**
   * {@code float.ToString()}.
   *
   * @param value Wert
   * @return Text im Format „G7“
   */
  public static String toString(float value) {
    return general(value, 7);
  }

  /**
   * {@code double.ToString()}.
   *
   * @param value Wert
   * @return Text im Format „G15“
   */
  public static String toString(double value) {
    return general(value, 15);
  }

  /**
   * {@code int.ToString()}.
   *
   * @param value Wert
   * @return Dezimaltext
   */
  public static String toString(int value) {
    return Integer.toString(value);
  }

  /**
   * {@code long.ToString()}.
   *
   * @param value Wert
   * @return Dezimaltext
   */
  public static String toString(long value) {
    return Long.toString(value);
  }

  /**
   * {@code bool.ToString()}.
   *
   * @param value Wert
   * @return „True“ oder „False“
   */
  public static String toString(boolean value) {
    return value ? "True" : "False";
  }

  /**
   * {@code char.ToString()}.
   *
   * @param value Zeichen
   * @return Text aus einem Zeichen
   */
  public static String toString(char value) {
    return String.valueOf(value);
  }

  /**
   * {@code object.ToString()} mit .NET-Formaten für Zahlen und {@code bool}; {@code null} ergibt „“
   * (wie in String-Verkettungen).
   *
   * @param value Wert oder {@code null}
   * @return Text
   */
  public static String toString(Object value) {
    return switch (value) {
      case null -> "";
      case String s -> s;
      case Float f -> toString(f.floatValue());
      case Double d -> toString(d.doubleValue());
      case Boolean b -> toString(b.booleanValue());
      default -> value.toString();
    };
  }

  /** Format „G“ mit {@code precision} signifikanten Stellen. */
  private static String general(double value, int precision) {
    if (Double.isNaN(value)) {
      return "NaN";
    }
    if (Double.isInfinite(value)) {
      return value > 0 ? "Infinity" : "-Infinity";
    }
    if (value == 0d) {
      return "0";
    }
    BigDecimal rounded =
        new BigDecimal(value)
            .round(new MathContext(precision, RoundingMode.HALF_UP))
            .stripTrailingZeros();
    int exponent = rounded.precision() - rounded.scale() - 1;
    if (exponent >= precision || exponent < -4) {
      return scientific(rounded, exponent);
    }
    return rounded.toPlainString();
  }

  private static String scientific(BigDecimal rounded, int exponent) {
    String digits = rounded.unscaledValue().abs().toString();
    StringBuilder out = new StringBuilder();
    if (rounded.signum() < 0) {
      out.append('-');
    }
    out.append(digits.charAt(0));
    if (digits.length() > 1) {
      out.append('.').append(digits, 1, digits.length());
    }
    out.append('E').append(exponent < 0 ? '-' : '+');
    int magnitude = java.lang.Math.abs(exponent);
    if (magnitude < 10) {
      out.append('0');
    }
    return out.append(magnitude).toString();
  }
}
