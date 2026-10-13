package io.github.localsoul.port.engine.api.math;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VecTest {

  @Test
  void crossOfXAndYIsZRightHanded() {
    assertEquals(Vec3.UNIT_Z, Vec3.UNIT_X.cross(Vec3.UNIT_Y));
    assertEquals(Vec3.UNIT_X, Vec3.UNIT_Y.cross(Vec3.UNIT_Z));
    assertEquals(Vec3.UNIT_Y, Vec3.UNIT_Z.cross(Vec3.UNIT_X));
    assertEquals(new Vec3(0f, 0f, -1f), Vec3.UNIT_Y.cross(Vec3.UNIT_X));
  }

  @Test
  void crossOfParallelVectorsIsZero() {
    Vec3 v = new Vec3(1f, 2f, 3f);
    assertEquals(Vec3.ZERO, v.cross(v.scale(2f)));
  }

  @Test
  void addSubScaleMultiply() {
    Vec3 a = new Vec3(1f, 2f, 3f);
    Vec3 b = new Vec3(4f, -5f, 0.5f);
    assertEquals(new Vec3(5f, -3f, 3.5f), a.add(b));
    assertEquals(new Vec3(-3f, 7f, 2.5f), a.sub(b));
    assertEquals(new Vec3(2f, 4f, 6f), a.scale(2f));
    assertEquals(new Vec3(4f, -10f, 1.5f), a.multiply(b));
    assertEquals(new Vec2(4f, 6f), new Vec2(1f, 2f).add(new Vec2(3f, 4f)));
    assertEquals(new Vec2(-2f, -2f), new Vec2(1f, 2f).sub(new Vec2(3f, 4f)));
    assertEquals(new Vec2(0.5f, 1f), new Vec2(1f, 2f).scale(0.5f));
  }

  @Test
  void dotAndLength() {
    assertEquals(32f, new Vec3(1f, 2f, 3f).dot(new Vec3(4f, 5f, 6f)));
    assertEquals(0f, Vec3.UNIT_X.dot(Vec3.UNIT_Y));
    assertEquals(5f, new Vec3(3f, 4f, 0f).length());
    assertEquals(25f, new Vec3(3f, 4f, 0f).lengthSquared());
    assertEquals(5f, new Vec2(3f, 4f).length());
    assertEquals(11f, new Vec2(1f, 2f).dot(new Vec2(3f, 4f)));
  }

  @Test
  void normalizesToUnitLength() {
    Vec3 n = new Vec3(3f, 4f, 12f).normalized();
    assertEquals(1f, n.length(), MathConstants.EPSILON);
    assertTrue(n.approxEquals(new Vec3(3f / 13f, 4f / 13f, 12f / 13f)));
    assertTrue(new Vec2(0f, -7f).normalized().approxEquals(new Vec2(0f, -1f)));
  }

  @Test
  void normalizingZeroVectorGivesZeroWithoutNaN() {
    assertEquals(Vec3.ZERO, Vec3.ZERO.normalized());
    assertEquals(Vec2.ZERO, Vec2.ZERO.normalized());
  }

  @Test
  void approxEqualsUsesEpsilonPerComponent() {
    Vec3 a = new Vec3(1f, 1f, 1f);
    assertTrue(a.approxEquals(new Vec3(1f + 0.5e-5f, 1f, 1f - 0.5e-5f)));
    assertFalse(a.approxEquals(new Vec3(1f + 2e-5f, 1f, 1f)));
    assertTrue(a.approxEquals(new Vec3(1.1f, 1f, 1f), 0.2f));
    assertTrue(new Vec2(1f, 1f).approxEquals(new Vec2(1f, 1f + 1e-6f)));
    assertFalse(new Vec2(1f, 1f).approxEquals(new Vec2(1f, 1.1f)));
  }

  @Test
  void approxEqualsHelperIsSymmetricAndInclusive() {
    assertTrue(MathConstants.approxEquals(1f, 1.5f, 0.5f));
    assertTrue(MathConstants.approxEquals(1.5f, 1f, 0.5f));
    assertFalse(MathConstants.approxEquals(1f, 1.6f, 0.5f));
  }
}
