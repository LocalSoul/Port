package io.github.localsoul.runtime.system;

/**
 * Array-Hilfen für generierten Code. {@code filled} belegt Arrays von Werttypen mit ihrem
 * Standardwert, weil unveränderliche Records keinen {@code null}-freien Ausgangszustand haben
 * (ADR-009); C# legt {@code new Vector3[n]} mit Nullwerten an.
 */
public final class RuntimeArrays {

  private RuntimeArrays() {}

  /**
   * Füllt ein Array mit einem Wert.
   *
   * @param <T> Elementtyp
   * @param array Array
   * @param value Wert für jedes Element
   * @return dasselbe Array
   */
  public static <T> T[] filled(T[] array, T value) {
    for (int i = 0; i < array.length; i++) {
      array[i] = value;
    }
    return array;
  }
}
