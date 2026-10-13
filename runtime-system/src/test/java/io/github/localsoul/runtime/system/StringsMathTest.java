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
        "Value does not fall within the expected range.", new ArgumentException().message());
    assertEquals(
        "Exception of type 'System.Exception' was thrown.", new SystemException().message());
    assertEquals(
        "Value cannot be null.\nParameter name: key", new ArgumentNullException("key").message());
    assertEquals("key", new ArgumentNullException("key").paramName());
    assertEquals(
        "zu groß\nParameter name: index",
        new ArgumentOutOfRangeException("index", "zu groß").message());
    assertEquals("kaputt\nParameter name: x", new ArgumentException("kaputt", "x").message());
    assertEquals("kaputt", new InvalidOperationException("kaputt").getMessage());
    assertTrue(new ArgumentNullException() instanceof ArgumentException);
    assertTrue(new ArgumentException() instanceof SystemException);
    assertTrue(new SystemException() instanceof RuntimeException);
    assertEquals(
        "The given key was not present in the dictionary.",
        new io.github.localsoul.runtime.system.collections.generic.KeyNotFoundException()
            .message());
  }

  @Test
  void javaExceptionsTranslateToDotNetTypes() {
    SystemException fromNpe = SystemException.fromJava(new NullPointerException());
    assertTrue(fromNpe instanceof NullReferenceException);
    assertEquals("Object reference not set to an instance of an object.", fromNpe.message());
    assertTrue(
        SystemException.fromJava(new ArrayIndexOutOfBoundsException())
            instanceof IndexOutOfRangeException);
    assertEquals(
        "Index was outside the bounds of the array.",
        IndexOutOfRangeException.fromJava(new ArrayIndexOutOfBoundsException()).message());
    InvalidOperationException own = new InvalidOperationException("x");
    assertTrue(SystemException.fromJava(own) == own);
    assertThrows(
        ArithmeticException.class, () -> SystemException.fromJava(new ArithmeticException()));
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
