package io.github.localsoul.runtime.system;

/** {@code System.Object}: statische Methoden und Hilfen für {@code ToString}/{@code Equals}. */
public final class SystemObject {

  private SystemObject() {}

  /**
   * {@code object.ReferenceEquals(a, b)}.
   *
   * @param a erstes Objekt
   * @param b zweites Objekt
   * @return {@code true} bei gleicher Referenz
   */
  public static boolean referenceEquals(Object a, Object b) {
    return a == b;
  }

  /**
   * {@code x.ToString()} mit .NET-Formaten.
   *
   * @param receiver Objekt
   * @return Text
   * @throws NullReferenceException wenn {@code receiver} {@code null} ist
   */
  public static String toString(Object receiver) {
    if (receiver == null) {
      throw new NullReferenceException("Object reference not set to an instance of an object.");
    }
    return Convert.toString(receiver);
  }

  /**
   * {@code x.Equals(other)} mit .NET-Gleichheit.
   *
   * @param receiver Objekt
   * @param other anderes Objekt
   * @return Gleichheit
   * @throws NullReferenceException wenn {@code receiver} {@code null} ist
   */
  public static boolean equals(Object receiver, Object other) {
    if (receiver == null) {
      throw new NullReferenceException("Object reference not set to an instance of an object.");
    }
    return Equality.equals(receiver, other);
  }
}
