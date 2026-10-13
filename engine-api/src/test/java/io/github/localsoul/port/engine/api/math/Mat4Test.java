package io.github.localsoul.port.engine.api.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Mat4Test {

  private static final float HALF_PI = (float) (Math.PI / 2.0);

  @Test
  void identityKeepsPoints() {
    Vec3 p = new Vec3(1f, 2f, 3f);
    assertEquals(p, Mat4.identity().transformPoint(p));
    assertEquals(Mat4.identity(), Mat4.trs(Vec3.ZERO, Quat.IDENTITY, Vec3.ONE));
  }

  @Test
  void trsScalesThenRotatesThenTranslates() {
    Mat4 m =
        Mat4.trs(
            new Vec3(10f, 0f, 0f), Quat.fromAxisAngle(Vec3.UNIT_Y, HALF_PI), new Vec3(2f, 2f, 2f));
    // (1,0,0) -> skaliert (2,0,0) -> gedreht (0,0,-2) -> verschoben (10,0,-2)
    assertTrue(m.transformPoint(Vec3.UNIT_X).approxEquals(new Vec3(10f, 0f, -2f)));
    assertTrue(m.transformDirection(Vec3.UNIT_X).approxEquals(new Vec3(0f, 0f, -2f)));
    assertEquals(new Vec3(10f, 0f, 0f), m.translation());
  }

  @Test
  void multiplicationAppliesRightFactorFirst() {
    Mat4 translate = Mat4.trs(new Vec3(0f, 5f, 0f), Quat.IDENTITY, Vec3.ONE);
    Mat4 scale = Mat4.trs(Vec3.ZERO, Quat.IDENTITY, new Vec3(3f, 3f, 3f));
    assertEquals(new Vec3(3f, 8f, 3f), translate.multiply(scale).transformPoint(Vec3.ONE));
    assertEquals(new Vec3(3f, 18f, 3f), scale.multiply(translate).transformPoint(Vec3.ONE));
  }

  @Test
  void multiplyWithIdentityIsNeutral() {
    Mat4 m =
        Mat4.trs(new Vec3(1f, 2f, 3f), Quat.fromAxisAngle(Vec3.UNIT_X, 0.3f), new Vec3(1f, 2f, 3f));
    assertEquals(m, m.multiply(Mat4.identity()));
    assertEquals(m, Mat4.identity().multiply(m));
  }

  @Test
  void rotationMatrixMatchesQuaternionRotation() {
    Quat q = Quat.fromAxisAngle(new Vec3(1f, 1f, 0f), 0.9f);
    Vec3 v = new Vec3(0.2f, -1f, 3f);
    Vec3 viaMatrix = Mat4.trs(Vec3.ZERO, q, Vec3.ONE).transformPoint(v);
    assertTrue(viaMatrix.approxEquals(q.rotate(v)), viaMatrix + " vs " + q.rotate(v));
  }

  @Test
  void columnMajorRoundtripAndAccess() {
    float[] values = new float[16];
    for (int i = 0; i < 16; i++) {
      values[i] = i;
    }
    Mat4 m = Mat4.ofColumnMajor(values);
    assertEquals(4f, m.get(0, 1));
    assertEquals(1f, m.get(1, 0));
    values[0] = 99f;
    assertEquals(0f, m.get(0, 0), "Eingabe wird kopiert");
    float[] out = m.toColumnMajor();
    out[1] = 99f;
    assertEquals(1f, m.get(1, 0), "Ausgabe ist eine Kopie");
  }

  @Test
  void rejectsInvalidInput() {
    assertThrows(IllegalArgumentException.class, () -> Mat4.ofColumnMajor(1f, 2f));
    assertThrows(IndexOutOfBoundsException.class, () -> Mat4.identity().get(4, 0));
    assertThrows(IndexOutOfBoundsException.class, () -> Mat4.identity().get(0, -1));
  }

  @Test
  void equalityIsExactAndApproxEqualsTolerant() {
    Mat4 a = Mat4.trs(new Vec3(1f, 0f, 0f), Quat.IDENTITY, Vec3.ONE);
    Mat4 b = Mat4.trs(new Vec3(1f + 1e-6f, 0f, 0f), Quat.IDENTITY, Vec3.ONE);
    assertNotEquals(a, b);
    assertTrue(a.approxEquals(b));
    assertEquals(a.hashCode(), Mat4.trs(new Vec3(1f, 0f, 0f), Quat.IDENTITY, Vec3.ONE).hashCode());
  }

  @Test
  void toStringIsRowWiseAndDeterministic() {
    assertEquals(
        "Mat4[1.0, 0.0, 0.0, 2.0; 0.0, 1.0, 0.0, 0.0; 0.0, 0.0, 1.0, 0.0; 0.0, 0.0, 0.0, 1.0]",
        Mat4.trs(new Vec3(2f, 0f, 0f), Quat.IDENTITY, Vec3.ONE).toString());
  }
}
