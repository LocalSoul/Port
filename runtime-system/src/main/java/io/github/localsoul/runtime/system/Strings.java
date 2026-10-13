package io.github.localsoul.runtime.system;

/**
 * Statische Seite von {@code System.String} und Hilfen für Instanzmethoden mit anderer Semantik als
 * Java ({@code Substring}). Gleichheit ist ordinal (wie {@code string ==} in C#).
 */
public final class Strings {

  private Strings() {}

  /**
   * {@code string == string}.
   *
   * @param a erster Text oder {@code null}
   * @param b zweiter Text oder {@code null}
   * @return ordinale Gleichheit, {@code null == null} ist wahr
   */
  public static boolean opEquality(String a, String b) {
    return a == null ? b == null : a.equals(b);
  }

  /**
   * {@code string != string}.
   *
   * @param a erster Text oder {@code null}
   * @param b zweiter Text oder {@code null}
   * @return Gegenteil von {@link #opEquality}
   */
  public static boolean opInequality(String a, String b) {
    return !opEquality(a, b);
  }

  /**
   * {@code string.Concat(string, string)}: {@code null} zählt als „“.
   *
   * @param a erster Text
   * @param b zweiter Text
   * @return Verkettung
   */
  public static String concat(String a, String b) {
    return (a == null ? "" : a) + (b == null ? "" : b);
  }

  /**
   * {@code string.Concat(object, object)}: Werte über {@link Convert#toString(Object)}.
   *
   * @param a erster Wert
   * @param b zweiter Wert
   * @return Verkettung
   */
  public static String concat(Object a, Object b) {
    return Convert.toString(a) + Convert.toString(b);
  }

  /**
   * {@code string.IsNullOrEmpty}.
   *
   * @param value Text oder {@code null}
   * @return {@code true} bei {@code null} oder leer
   */
  public static boolean isNullOrEmpty(String value) {
    return value == null || value.isEmpty();
  }

  /**
   * {@code s.Substring(start)}.
   *
   * @param s Text
   * @param start Startindex
   * @return Rest ab {@code start}
   * @throws NullReferenceException wenn {@code s} {@code null} ist
   * @throws ArgumentOutOfRangeException bei ungültigem Start
   */
  public static String substring(String s, int start) {
    String text = receiver(s);
    return substring(text, start, text.length() - start);
  }

  /**
   * {@code s.Substring(start, length)} (.NET: Länge, nicht Endindex).
   *
   * @param s Text
   * @param start Startindex
   * @param length Länge
   * @return Teiltext
   * @throws NullReferenceException wenn {@code s} {@code null} ist
   * @throws ArgumentOutOfRangeException bei ungültigen Grenzen
   */
  public static String substring(String s, int start, int length) {
    String text = receiver(s);
    if (start < 0 || length < 0 || start > text.length() - length) {
      throw new ArgumentOutOfRangeException(
          "Index and length must refer to a location within the string.");
    }
    return text.substring(start, start + length);
  }

  /**
   * {@code s.ToString()}.
   *
   * @param s Text
   * @return derselbe Text
   * @throws NullReferenceException wenn {@code s} {@code null} ist
   */
  public static String toString(String s) {
    return receiver(s);
  }

  private static String receiver(String s) {
    if (s == null) {
      throw new NullReferenceException("Object reference not set to an instance of an object.");
    }
    return s;
  }
}
