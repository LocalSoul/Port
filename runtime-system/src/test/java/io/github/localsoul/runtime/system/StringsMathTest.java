package io.github.localsoul.runtime.system;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Strings, Math und Exceptions mit .NET-Semantik. */
class StringsMathTest {

  @Test
  void stringEqualityIsOrdinalAndNullSafe() {
    assertTrue(Strings.opEquality(null, null));
    assertFalse(Strings.opEquality("a", null));
    assertTrue(Strings.opEquality("ä", new String("ä")));
    assertTrue(Strings.opInequality("a", "A"));
  }

  @Test
  void concatenationTreatsNullAsEmptyAndFormatsNumbersLikeDotNet() {
    assertEquals("a", Strings.concat((String) null, "a"));
    assertEquals("Score: 5", Strings.concat("Score: ", (Object) 5f));
    assertEquals("x0.3333333", Strings.concat("x", (Object) (1f / 3f)));
    assertEquals("True", Strings.concat(null, (Object) true));
  }

  @Test
  void substringUsesStartAndLength() {
    assertEquals("ell", Strings.substring("hello", 1, 3));
    assertEquals("llo", Strings.substring("hello", 2));
    assertThrows(ArgumentOutOfRangeException.class, () -> Strings.substring("hello", 3, 5));
    assertThrows(ArgumentOutOfRangeException.class, () -> Strings.substring("hello", -1, 1));
    assertThrows(NullReferenceException.class, () -> Strings.substring(null, 0));
    assertTrue(Strings.isNullOrEmpty(""));
    assertTrue(Strings.isNullOrEmpty(null));
  }

  @Test
  void roundUsesBankersRounding() {
    assertEquals(2.0, Math.round(2.5));
    assertEquals(4.0, Math.round(3.5));
    assertEquals(-2.0, Math.round(-2.5));
    assertEquals(2.0f, MathF.round(2.5f));
    assertEquals(3.0, Math.round(2.6));
  }

  @Test
  void absOfMinValueOverflowsLikeDotNet() {
    assertThrows(OverflowException.class, () -> Math.abs(Integer.MIN_VALUE));
    assertEquals(5, Math.abs(-5));
  }

  @Test
  void clampChecksBounds() {
    assertEquals(10, Math.clamp(12, 0, 10));
    assertEquals(0.5f, Math.clamp(0.5f, 0f, 1f));
    assertThrows(ArgumentException.class, () -> Math.clamp(1, 5, 2));
  }

  @Test
  void floatMathIsDeterministicFloat() {
    assertEquals(1.4142135f, MathF.sqrt(2f));
    assertEquals((float) java.lang.Math.PI, MathF.PI());
    assertEquals(java.lang.Math.PI, Math.PI());
  }

  @Test
  void exceptionsHaveDotNetNamesAndDefaultMessages() {
    assertEquals(
        "Exception of type 'System.ArgumentException' was thrown.",
        new ArgumentException().message());
    assertEquals("kaputt", new InvalidOperationException("kaputt").getMessage());
    assertTrue(new ArgumentNullException() instanceof ArgumentException);
    assertTrue(new ArgumentException() instanceof SystemException);
    assertTrue(new SystemException() instanceof RuntimeException);
    assertEquals(
        "Exception of type 'System.Collections.Generic.KeyNotFoundException' was thrown.",
        new io.github.localsoul.runtime.system.collections.generic.KeyNotFoundException()
            .message());
  }

  @Test
  void objectHelpersFollowDotNet() {
    assertTrue(SystemObject.equals(0.0f, -0.0f));
    assertTrue(SystemObject.equals(Float.NaN, Float.NaN));
    assertEquals("5", SystemObject.toString(5f));
    assertThrows(NullReferenceException.class, () -> SystemObject.toString(null));
    Object a = new Object();
    assertTrue(SystemObject.referenceEquals(a, a));
    assertFalse(SystemObject.referenceEquals(a, new Object()));
  }

  @Test
  void delegatesInvoke() {
    Func1<Integer, Integer> twice = x -> x * 2;
    assertEquals(8, twice.invoke(4));
    StringBuilder log = new StringBuilder();
    Action1<String> append = log::append;
    append.invoke("ok");
    assertEquals("ok", log.toString());
  }
}
