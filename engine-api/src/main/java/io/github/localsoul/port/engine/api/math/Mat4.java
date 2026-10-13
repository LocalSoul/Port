package io.github.localsoul.port.engine.api.math;

import java.util.Arrays;

/**
 * Unveränderliche 4×4-Matrix ({@code float}) für affine Transformationen. Spaltenvektoren: ein
 * Punkt {@code p} wird als {@code M * (p, 1)} transformiert; {@code a.multiply(b)} wirkt erst
 * {@code b}, dann {@code a}. Interne Ablage spaltenweise. Rechts-händig, Y oben, Meter (ADR-007).
 *
 * <p>Gleichheit ({@link #equals}) ist exakt über alle 16 Werte; für Toleranzvergleiche {@link
 * #approxEquals} nutzen.
 */
public final class Mat4 {

  private static final Mat4 IDENTITY =
      new Mat4(new float[] {1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 0f, 1f});

  /** Spaltenweise: Element (Zeile r, Spalte c) liegt bei {@code c * 4 + r}. */
  private final float[] m;

  private Mat4(float[] values) {
    this.m = values;
  }

  /**
   * Einheitsmatrix.
   *
   * @return die Einheitsmatrix
   */
  public static Mat4 identity() {
    return IDENTITY;
  }

  /**
   * Erzeugt eine Matrix aus 16 Werten in spaltenweiser Reihenfolge.
   *
   * @param columnMajor genau 16 Werte, werden kopiert
   * @return Matrix
   * @throws IllegalArgumentException wenn nicht genau 16 Werte übergeben werden
   */
  public static Mat4 ofColumnMajor(float... columnMajor) {
    if (columnMajor.length != 16) {
      throw new IllegalArgumentException("Mat4 braucht 16 Werte, nicht " + columnMajor.length);
    }
    return new Mat4(columnMajor.clone());
  }

  /**
   * Komposition aus Verschiebung, Rotation und Skalierung: {@code T * R * S} (erst skalieren, dann
   * drehen, dann verschieben).
   *
   * @param translation Verschiebung in Meter
   * @param rotation Rotation, normiert
   * @param scale Skalierung je Achse
   * @return Transformationsmatrix
   */
  public static Mat4 trs(Vec3 translation, Quat rotation, Vec3 scale) {
    float x = rotation.x();
    float y = rotation.y();
    float z = rotation.z();
    float w = rotation.w();
    float[] r = new float[16];
    r[0] = (1f - 2f * (y * y + z * z)) * scale.x();
    r[1] = (2f * (x * y + w * z)) * scale.x();
    r[2] = (2f * (x * z - w * y)) * scale.x();
    r[4] = (2f * (x * y - w * z)) * scale.y();
    r[5] = (1f - 2f * (x * x + z * z)) * scale.y();
    r[6] = (2f * (y * z + w * x)) * scale.y();
    r[8] = (2f * (x * z + w * y)) * scale.z();
    r[9] = (2f * (y * z - w * x)) * scale.z();
    r[10] = (1f - 2f * (x * x + y * y)) * scale.z();
    r[12] = translation.x();
    r[13] = translation.y();
    r[14] = translation.z();
    r[15] = 1f;
    return new Mat4(r);
  }

  /**
   * Element an Zeile und Spalte.
   *
   * @param row Zeile 0 bis 3
   * @param column Spalte 0 bis 3
   * @return Wert
   * @throws IndexOutOfBoundsException bei ungültigem Index
   */
  public float get(int row, int column) {
    if (row < 0 || row > 3 || column < 0 || column > 3) {
      throw new IndexOutOfBoundsException("Mat4[" + row + "," + column + "]");
    }
    return m[column * 4 + row];
  }

  /**
   * Matrixprodukt {@code this * other}: wirkt erst {@code other}, dann {@code this}.
   *
   * @param other rechter Faktor
   * @return Produkt
   */
  public Mat4 multiply(Mat4 other) {
    float[] result = new float[16];
    for (int column = 0; column < 4; column++) {
      for (int row = 0; row < 4; row++) {
        float sum = 0f;
        for (int k = 0; k < 4; k++) {
          sum += m[k * 4 + row] * other.m[column * 4 + k];
        }
        result[column * 4 + row] = sum;
      }
    }
    return new Mat4(result);
  }

  /**
   * Transformiert einen Punkt (mit Verschiebung, {@code w = 1}).
   *
   * @param point Punkt in Meter
   * @return transformierter Punkt
   */
  public Vec3 transformPoint(Vec3 point) {
    float px = m[0] * point.x() + m[4] * point.y() + m[8] * point.z() + m[12];
    float py = m[1] * point.x() + m[5] * point.y() + m[9] * point.z() + m[13];
    float pz = m[2] * point.x() + m[6] * point.y() + m[10] * point.z() + m[14];
    return new Vec3(px, py, pz);
  }

  /**
   * Transformiert eine Richtung (ohne Verschiebung, {@code w = 0}).
   *
   * @param direction Richtung
   * @return transformierte Richtung
   */
  public Vec3 transformDirection(Vec3 direction) {
    float dx = m[0] * direction.x() + m[4] * direction.y() + m[8] * direction.z();
    float dy = m[1] * direction.x() + m[5] * direction.y() + m[9] * direction.z();
    float dz = m[2] * direction.x() + m[6] * direction.y() + m[10] * direction.z();
    return new Vec3(dx, dy, dz);
  }

  /**
   * Verschiebungsanteil (4. Spalte).
   *
   * @return Verschiebung in Meter
   */
  public Vec3 translation() {
    return new Vec3(m[12], m[13], m[14]);
  }

  /**
   * Werte in spaltenweiser Reihenfolge.
   *
   * @return Kopie der 16 Werte
   */
  public float[] toColumnMajor() {
    return m.clone();
  }

  /**
   * Vergleicht alle Elemente mit Toleranz.
   *
   * @param other zweite Matrix
   * @param epsilon erlaubte Abweichung je Element
   * @return {@code true}, wenn alle Elemente höchstens {@code epsilon} abweichen
   */
  public boolean approxEquals(Mat4 other, float epsilon) {
    for (int i = 0; i < 16; i++) {
      if (!MathConstants.approxEquals(m[i], other.m[i], epsilon)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Vergleicht alle Elemente mit {@link MathConstants#EPSILON}.
   *
   * @param other zweite Matrix
   * @return {@code true} bei Gleichheit innerhalb der Standardtoleranz
   */
  public boolean approxEquals(Mat4 other) {
    return approxEquals(other, MathConstants.EPSILON);
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Mat4 other && Arrays.equals(m, other.m);
  }

  @Override
  public int hashCode() {
    return Arrays.hashCode(m);
  }

  /** Liefert die Werte zeilenweise, deterministisch über {@link Float#toString(float)}. */
  @Override
  public String toString() {
    StringBuilder out = new StringBuilder("Mat4[");
    for (int row = 0; row < 4; row++) {
      if (row > 0) {
        out.append("; ");
      }
      for (int column = 0; column < 4; column++) {
        if (column > 0) {
          out.append(", ");
        }
        out.append(Float.toString(get(row, column)));
      }
    }
    return out.append(']').toString();
  }
}
