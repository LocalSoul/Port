package io.github.localsoul.runtime.system;

/**
 * Gleichheit wie {@code EqualityComparer<T>.Default} in .NET. Unterschied zu Java: {@code 0.0f} und
 * {@code -0.0f} sind gleich ({@code float.Equals}), {@code NaN} ist sich selbst gleich (wie Java
 * {@code Float.equals}).
 */
public final class Equality {

  private Equality() {}

  /**
   * Vergleicht zwei Werte.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return {@code true} bei .NET-Gleichheit
   */
  public static boolean equals(Object a, Object b) {
    if (a == b) {
      return true;
    }
    if (a == null || b == null) {
      return false;
    }
    return key(a).equals(key(b));
  }

  /**
   * Schlüssel für Java-Hash-Strukturen: normalisiert {@code -0.0} zu {@code 0.0}, damit Hashwert
   * und Gleichheit zu .NET passen.
   *
   * @param value Wert, nicht {@code null}
   * @return normalisierter Wert
   */
  public static Object key(Object value) {
    if (value instanceof Float f && f == 0f) {
      return 0f;
    }
    if (value instanceof Double d && d == 0d) {
      return 0d;
    }
    return value;
  }
}
