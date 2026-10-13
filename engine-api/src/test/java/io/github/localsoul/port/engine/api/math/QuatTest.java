package io.github.localsoul.port.engine.api.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class QuatTest {

  private static final float HALF_PI = (float) (Math.PI / 2.0);

  @Test
  void identityDoesNotRotate() {
    Vec3 v = new Vec3(1f, -2f, 3f);
    assertEquals(v, Quat.IDENTITY.rotate(v));
    Quat q = Quat.fromAxisAngle(Vec3.UNIT_Y, 0.7f);
    assertEquals(q, Quat.IDENTITY.multiply(q));
    assertEquals(q, q.multiply(Quat.IDENTITY));
  }

  @Test
  void rotationAboutYMapsXToMinusZ() {
    Quat q = Quat.fromAxisAngle(Vec3.UNIT_Y, HALF_PI);
    assertTrue(
        q.rotate(Vec3.UNIT_X).approxEquals(Vec3.UNIT_Z.negate()), q.rotate(Vec3.UNIT_X).toString());
  }

  @Test
  void rotationAboutZMapsXToY() {
    Quat q = Quat.fromAxisAngle(Vec3.UNIT_Z, HALF_PI);
    assertTrue(q.rotate(Vec3.UNIT_X).approxEquals(Vec3.UNIT_Y));
  }

  @Test
  void multiplicationAppliesRightFactorFirst() {
    Quat aboutZ = Quat.fromAxisAngle(Vec3.UNIT_Z, HALF_PI);
    Quat aboutX = Quat.fromAxisAngle(Vec3.UNIT_X, HALF_PI);
    // erst Z (X -> Y), dann X (Y -> Z)
    Vec3 result = aboutX.multiply(aboutZ).rotate(Vec3.UNIT_X);
    assertTrue(result.approxEquals(Vec3.UNIT_Z), result.toString());
  }

  @Test
  void twoQuarterTurnsMakeAHalfTurn() {
    Quat quarter = Quat.fromAxisAngle(Vec3.UNIT_Y, HALF_PI);
    Quat half = Quat.fromAxisAngle(Vec3.UNIT_Y, (float) Math.PI);
    assertTrue(quarter.multiply(quarter).sameRotation(half));
  }

  @Test
  void conjugateUndoesRotation() {
    Quat q = Quat.fromAxisAngle(new Vec3(1f, 2f, 3f), 1.2f);
    Vec3 v = new Vec3(0.3f, -4f, 2f);
    assertTrue(q.conjugate().rotate(q.rotate(v)).approxEquals(v));
    assertTrue(q.multiply(q.conjugate()).approxEquals(Quat.IDENTITY));
  }

  @Test
  void fromAxisAngleNormalizesAxisAndResult() {
    Quat q = Quat.fromAxisAngle(new Vec3(0f, 5f, 0f), HALF_PI);
    assertEquals(1f, q.length(), MathConstants.EPSILON);
    assertTrue(q.approxEquals(Quat.fromAxisAngle(Vec3.UNIT_Y, HALF_PI)));
  }

  @Test
  void zeroAxisGivesIdentity() {
    assertEquals(Quat.IDENTITY, Quat.fromAxisAngle(Vec3.ZERO, 1f));
  }

  @Test
  void normalizesAndHandlesZero() {
    Quat q = new Quat(0f, 0f, 3f, 4f).normalized();
    assertTrue(q.approxEquals(new Quat(0f, 0f, 0.6f, 0.8f)));
    assertEquals(Quat.IDENTITY, new Quat(0f, 0f, 0f, 0f).normalized());
  }

  @Test
  void sameRotationAcceptsNegatedQuaternion() {
    Quat q = Quat.fromAxisAngle(Vec3.UNIT_X, 0.5f);
    Quat negated = new Quat(-q.x(), -q.y(), -q.z(), -q.w());
    assertFalse(q.approxEquals(negated));
    assertTrue(q.sameRotation(negated));
    assertFalse(q.sameRotation(Quat.IDENTITY));
  }

  @Test
  void fromAxisAngleIsDeterministic() {
    Quat a = Quat.fromAxisAngle(new Vec3(0.3f, 0.4f, 0.5f), 2.1f);
    Quat b = Quat.fromAxisAngle(new Vec3(0.3f, 0.4f, 0.5f), 2.1f);
    assertEquals(a, b);
  }
}
