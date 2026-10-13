package io.github.localsoul.port.engine.api.math;

/**
 * Unveränderliches Quaternion {@code (x, y, z, w)} für Rotationen. Rechts-händig: eine positive
 * Drehung um eine Achse ist gegen den Uhrzeigersinn, wenn man von der Achsenspitze auf den Ursprung
 * schaut (ADR-007). Winkel in Radiant.
 *
 * <p>Rotationsquaternionen sollten normiert sein; {@link #fromAxisAngle} und {@link #multiply}
 * normierter Werte liefern normierte Ergebnisse (bis auf Rundung).
 *
 * <p>{@code equals} ist exakt wie bei allen Records mit {@code float}-Komponenten ({@link
 * Float#compare}): {@code -0.0f} und {@code 0.0f} gelten als verschieden, {@code NaN} als gleich.
 * Für Rechenergebnisse {@code approxEquals} nutzen.
 *
 * @param x Vektoranteil X
 * @param y Vektoranteil Y
 * @param z Vektoranteil Z
 * @param w Skalaranteil
 */
public record Quat(float x, float y, float z, float w) {

  /** Keine Rotation. */
  public static final Quat IDENTITY = new Quat(0f, 0f, 0f, 1f);

  /**
   * Rotation um eine Achse.
   *
   * @param axis Drehachse, wird normiert; Nullvektor ergibt {@link #IDENTITY}
   * @param radians Winkel in Radiant, positiv = gegen den Uhrzeigersinn (rechts-händig)
   * @return normiertes Quaternion
   */
  public static Quat fromAxisAngle(Vec3 axis, float radians) {
    Vec3 unit = axis.normalized();
    if (unit.equals(Vec3.ZERO)) {
      return IDENTITY;
    }
    double half = radians * 0.5;
    float sin = (float) StrictMath.sin(half);
    float cos = (float) StrictMath.cos(half);
    return new Quat(unit.x() * sin, unit.y() * sin, unit.z() * sin, cos);
  }

  /**
   * Hamilton-Produkt {@code this * other}: wirkt erst {@code other}, dann {@code this}.
   *
   * @param other rechter Faktor
   * @return Produkt
   */
  public Quat multiply(Quat other) {
    float nx = w * other.x + x * other.w + y * other.z - z * other.y;
    float ny = w * other.y - x * other.z + y * other.w + z * other.x;
    float nz = w * other.z + x * other.y - y * other.x + z * other.w;
    float nw = w * other.w - x * other.x - y * other.y - z * other.z;
    return new Quat(nx, ny, nz, nw);
  }

  /**
   * Konjugiertes Quaternion; für normierte Quaternionen die inverse Rotation.
   *
   * @return {@code (-x, -y, -z, w)}
   */
  public Quat conjugate() {
    return new Quat(-x, -y, -z, w);
  }

  /**
   * Länge (Norm).
   *
   * @return {@code sqrt(x²+y²+z²+w²)}
   */
  public float length() {
    return MathConstants.sqrt(x * x + y * y + z * z + w * w);
  }

  /**
   * Normiertes Quaternion.
   *
   * @return Quaternion mit Länge 1; {@link #IDENTITY}, wenn die Länge 0 ist
   */
  public Quat normalized() {
    float length = length();
    if (length == 0f) {
      return IDENTITY;
    }
    float inverse = 1f / length;
    return new Quat(x * inverse, y * inverse, z * inverse, w * inverse);
  }

  /**
   * Dreht einen Vektor ({@code q v q*}), für normierte Quaternionen.
   *
   * @param v Vektor
   * @return gedrehter Vektor
   */
  public Vec3 rotate(Vec3 v) {
    Vec3 axis = new Vec3(x, y, z);
    Vec3 t = axis.cross(v).scale(2f);
    return v.add(t.scale(w)).add(axis.cross(t));
  }

  /**
   * Vergleicht komponentenweise mit Toleranz. Beachte: {@code q} und {@code -q} beschreiben
   * dieselbe Rotation, gelten hier aber als verschieden; dafür {@link #sameRotation} nutzen.
   *
   * @param other zweites Quaternion
   * @param epsilon erlaubte Abweichung je Komponente
   * @return {@code true}, wenn alle Komponenten höchstens {@code epsilon} abweichen
   */
  public boolean approxEquals(Quat other, float epsilon) {
    return MathConstants.approxEquals(x, other.x, epsilon)
        && MathConstants.approxEquals(y, other.y, epsilon)
        && MathConstants.approxEquals(z, other.z, epsilon)
        && MathConstants.approxEquals(w, other.w, epsilon);
  }

  /**
   * Vergleicht komponentenweise mit {@link MathConstants#EPSILON}.
   *
   * @param other zweites Quaternion
   * @return {@code true} bei Gleichheit innerhalb der Standardtoleranz
   */
  public boolean approxEquals(Quat other) {
    return approxEquals(other, MathConstants.EPSILON);
  }

  /**
   * Prüft, ob beide Quaternionen dieselbe Rotation beschreiben ({@code q} oder {@code -q}).
   *
   * @param other zweites Quaternion
   * @return {@code true} bei gleicher Rotation innerhalb von {@link MathConstants#EPSILON}
   */
  public boolean sameRotation(Quat other) {
    Quat negated = new Quat(-other.x, -other.y, -other.z, -other.w);
    return approxEquals(other) || approxEquals(negated);
  }
}
