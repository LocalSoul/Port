package io.github.localsoul.port.engine.api.math;

/**
 * Unveränderlicher 2D-Vektor ({@code float}), Einheit je nach Verwendung meist Meter.
 *
 * <p>{@code equals} ist exakt wie bei allen Records mit {@code float}-Komponenten ({@link
 * Float#compare}): {@code -0.0f} und {@code 0.0f} gelten als verschieden, {@code NaN} als gleich.
 * Für Rechenergebnisse {@code approxEquals} nutzen.
 *
 * @param x X-Komponente
 * @param y Y-Komponente (oben)
 */
public record Vec2(float x, float y) {

  /** {@code (0, 0)}. */
  public static final Vec2 ZERO = new Vec2(0f, 0f);

  /** {@code (1, 1)}. */
  public static final Vec2 ONE = new Vec2(1f, 1f);

  /** {@code (1, 0)}. */
  public static final Vec2 UNIT_X = new Vec2(1f, 0f);

  /** {@code (0, 1)}. */
  public static final Vec2 UNIT_Y = new Vec2(0f, 1f);

  /**
   * Addiert komponentenweise.
   *
   * @param other zweiter Summand
   * @return {@code this + other}
   */
  public Vec2 add(Vec2 other) {
    return new Vec2(x + other.x, y + other.y);
  }

  /**
   * Subtrahiert komponentenweise.
   *
   * @param other Subtrahend
   * @return {@code this - other}
   */
  public Vec2 sub(Vec2 other) {
    return new Vec2(x - other.x, y - other.y);
  }

  /**
   * Skaliert mit einem Faktor.
   *
   * @param factor Faktor
   * @return {@code this * factor}
   */
  public Vec2 scale(float factor) {
    return new Vec2(x * factor, y * factor);
  }

  /**
   * Skalarprodukt.
   *
   * @param other zweiter Vektor
   * @return {@code x*other.x + y*other.y}
   */
  public float dot(Vec2 other) {
    return x * other.x + y * other.y;
  }

  /**
   * Quadrierte Länge, ohne Wurzel.
   *
   * @return {@code x*x + y*y}
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
  public Vec2 normalized() {
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
  public boolean approxEquals(Vec2 other, float epsilon) {
    return MathConstants.approxEquals(x, other.x, epsilon)
        && MathConstants.approxEquals(y, other.y, epsilon);
  }

  /**
   * Vergleicht mit {@link MathConstants#EPSILON}.
   *
   * @param other zweiter Vektor
   * @return {@code true} bei Gleichheit innerhalb der Standardtoleranz
   */
  public boolean approxEquals(Vec2 other) {
    return approxEquals(other, MathConstants.EPSILON);
  }
}
