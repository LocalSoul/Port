package io.github.localsoul.port.engine.api.math;

/**
 * Unveränderlicher 3D-Vektor ({@code float}). Rechts-händiges System, Y oben (ADR-007); als
 * Position oder Verschiebung in Meter.
 *
 * <p>{@code equals} ist exakt wie bei allen Records mit {@code float}-Komponenten ({@link
 * Float#compare}): {@code -0.0f} und {@code 0.0f} gelten als verschieden, {@code NaN} als gleich.
 * Für Rechenergebnisse {@code approxEquals} nutzen.
 *
 * @param x X-Komponente
 * @param y Y-Komponente (oben)
 * @param z Z-Komponente (zum Betrachter, rechts-händig)
 */
public record Vec3(float x, float y, float z) {

  /** {@code (0, 0, 0)}. */
  public static final Vec3 ZERO = new Vec3(0f, 0f, 0f);

  /** {@code (1, 1, 1)}, z. B. als neutrale Skalierung. */
  public static final Vec3 ONE = new Vec3(1f, 1f, 1f);

  /** {@code (1, 0, 0)}. */
  public static final Vec3 UNIT_X = new Vec3(1f, 0f, 0f);

  /** {@code (0, 1, 0)}, oben. */
  public static final Vec3 UNIT_Y = new Vec3(0f, 1f, 0f);

  /** {@code (0, 0, 1)}. */
  public static final Vec3 UNIT_Z = new Vec3(0f, 0f, 1f);

  /**
   * Addiert komponentenweise.
   *
   * @param other zweiter Summand
   * @return {@code this + other}
   */
  public Vec3 add(Vec3 other) {
    return new Vec3(x + other.x, y + other.y, z + other.z);
  }

  /**
   * Subtrahiert komponentenweise.
   *
   * @param other Subtrahend
   * @return {@code this - other}
   */
  public Vec3 sub(Vec3 other) {
    return new Vec3(x - other.x, y - other.y, z - other.z);
  }

  /**
   * Skaliert mit einem Faktor.
   *
   * @param factor Faktor
   * @return {@code this * factor}
   */
  public Vec3 scale(float factor) {
    return new Vec3(x * factor, y * factor, z * factor);
  }

  /**
   * Multipliziert komponentenweise (z. B. für nicht-uniforme Skalierung).
   *
   * @param other Faktoren je Achse
   * @return {@code (x*other.x, y*other.y, z*other.z)}
   */
  public Vec3 multiply(Vec3 other) {
    return new Vec3(x * other.x, y * other.y, z * other.z);
  }

  /**
   * Gegenvektor.
   *
   * @return {@code -this}
   */
  public Vec3 negate() {
    return new Vec3(-x, -y, -z);
  }

  /**
   * Skalarprodukt.
   *
   * @param other zweiter Vektor
   * @return {@code x*other.x + y*other.y + z*other.z}
   */
  public float dot(Vec3 other) {
    return x * other.x + y * other.y + z * other.z;
  }

  /**
   * Kreuzprodukt, rechts-händig: {@code UNIT_X.cross(UNIT_Y) == UNIT_Z}.
   *
   * @param other zweiter Vektor
   * @return {@code this × other}
   */
  public Vec3 cross(Vec3 other) {
    float cx = y * other.z - z * other.y;
    float cy = z * other.x - x * other.z;
    float cz = x * other.y - y * other.x;
    return new Vec3(cx, cy, cz);
  }

  /**
   * Quadrierte Länge, ohne Wurzel.
   *
   * @return {@code x*x + y*y + z*z}
   */
  public float lengthSquared() {
    return dot(this);
  }

  /**
   * Euklidische Länge.
   *
   * @return Länge, nicht negativ
   */
  public float length() {
    return MathConstants.sqrt(lengthSquared());
  }

  /**
   * Vektor gleicher Richtung mit Länge 1.
   *
   * @return normierter Vektor; {@link #ZERO}, wenn die Länge 0 ist (kein Fehler, keine NaN-Werte)
   */
  public Vec3 normalized() {
    float length = length();
    if (length == 0f) {
      return ZERO;
    }
    return scale(1f / length);
  }

  /**
   * Vergleicht mit Toleranz je Komponente.
   *
   * @param other zweiter Vektor
   * @param epsilon erlaubte Abweichung je Komponente
   * @return {@code true}, wenn alle Komponenten höchstens {@code epsilon} abweichen
   */
  public boolean approxEquals(Vec3 other, float epsilon) {
    return MathConstants.approxEquals(x, other.x, epsilon)
        && MathConstants.approxEquals(y, other.y, epsilon)
        && MathConstants.approxEquals(z, other.z, epsilon);
  }

  /**
   * Vergleicht mit {@link MathConstants#EPSILON}.
   *
   * @param other zweiter Vektor
   * @return {@code true} bei Gleichheit innerhalb der Standardtoleranz
   */
  public boolean approxEquals(Vec3 other) {
    return approxEquals(other, MathConstants.EPSILON);
  }
}
