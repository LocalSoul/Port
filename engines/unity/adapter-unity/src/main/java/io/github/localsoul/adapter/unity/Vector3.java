package io.github.localsoul.adapter.unity;

import io.github.localsoul.port.engine.api.math.Vec3;

/**
 * {@code UnityEngine.Vector3}: unveränderlicher Wertetyp in Unity-Koordinaten (links-händig, Y
 * oben). Als Record entspricht er der C#-{@code struct}-Semantik für M3 (ADR-009 ist offen).
 *
 * @param x X-Komponente
 * @param y Y-Komponente
 * @param z Z-Komponente (Unity: vom Betrachter weg)
 */
public record Vector3(float x, float y, float z) {

  /** Unitys Toleranz für {@code ==}: Quadrat des Abstands kleiner als 1e-5². */
  private static final float EQUALITY_EPSILON_SQUARED = 9.99999944E-11f;

  /**
   * {@code Vector3 * float} ({@code op_Multiply}).
   *
   * @param a Vektor
   * @param d Faktor
   * @return Produkt
   */
  public static Vector3 mul(Vector3 a, float d) {
    return new Vector3(a.x * d, a.y * d, a.z * d);
  }

  /**
   * {@code Vector3 + Vector3} ({@code op_Addition}).
   *
   * @param a erster Summand
   * @param b zweiter Summand
   * @return Summe
   */
  public static Vector3 add(Vector3 a, Vector3 b) {
    return new Vector3(a.x + b.x, a.y + b.y, a.z + b.z);
  }

  /**
   * {@code Vector3 - Vector3} ({@code op_Subtraction}).
   *
   * @param a Minuend
   * @param b Subtrahend
   * @return Differenz
   */
  public static Vector3 sub(Vector3 a, Vector3 b) {
    return new Vector3(a.x - b.x, a.y - b.y, a.z - b.z);
  }

  /**
   * {@code Vector3 == Vector3} ({@code op_Equality}) mit Unitys Toleranz.
   *
   * @param a erster Vektor
   * @param b zweiter Vektor
   * @return {@code true}, wenn der quadrierte Abstand kleiner als 1e-10 ist
   */
  public static boolean equals(Vector3 a, Vector3 b) {
    float dx = a.x - b.x;
    float dy = a.y - b.y;
    float dz = a.z - b.z;
    return dx * dx + dy * dy + dz * dz < EQUALITY_EPSILON_SQUARED;
  }

  /**
   * Umrechnung in Core-Koordinaten (rechts-händig): Z wird gespiegelt (ADR-007). {@code 0f - z}
   * statt {@code -z}, damit aus 0 kein {@code -0.0} wird.
   */
  Vec3 toCore() {
    return new Vec3(x, y, 0f - z);
  }

  /** Umrechnung aus Core-Koordinaten: Z wird gespiegelt (ADR-007), ohne {@code -0.0}. */
  static Vector3 fromCore(Vec3 v) {
    return new Vector3(v.x(), v.y(), 0f - v.z());
  }
}
