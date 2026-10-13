package io.github.localsoul.runtime.system;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/**
 * Zahlformat wie .NET Framework / Unity-Mono („G7“ für float, „G15“ für double, invariant).
 * Erwartungswerte nach der .NET-Dokumentation zu Standard-Zahlenformaten („G“).
 */
class ConvertTest {

  @ParameterizedTest
  @CsvSource({
    "5.0, 5",
    "0.1, 0.1",
    "0.33333334, 0.3333333",
    "-2.5, -2.5",
    "1234567.0, 1234567",
    "12345678.0, 1.234568E+07",
    "10000000.0, 1E+07",
    "0.0001, 0.0001",
    "0.00001, 1E-05",
    "0.000012345, 1.2345E-05",
    "3.4028235E38, 3.402823E+38"
  })
  void formatsFloatsWithSevenSignificantDigits(float value, String expected) {
    assertEquals(expected, Convert.toString(value));
  }

  @Test
  void formatsSpecialFloatValues() {
    assertEquals("0", Convert.toString(-0.0f));
    assertEquals("NaN", Convert.toString(Float.NaN));
    assertEquals("Infinity", Convert.toString(Float.POSITIVE_INFINITY));
    assertEquals("-Infinity", Convert.toString(Float.NEGATIVE_INFINITY));
  }

  @Test
  void formatsDoublesWithFifteenSignificantDigits() {
    assertEquals("0.3", Convert.toString(0.1 + 0.2));
    assertEquals("0.333333333333333", Convert.toString(1.0 / 3.0));
    assertEquals("1E+15", Convert.toString(1e15));
    assertEquals("123456789012345", Convert.toString(123456789012345.0));
    assertEquals("5", Convert.toString(5.0));
  }

  @Test
  void formatsOtherPrimitivesLikeDotNet() {
    assertEquals("True", Convert.toString(true));
    assertEquals("False", Convert.toString(false));
    assertEquals("-42", Convert.toString(-42));
    assertEquals("x", Convert.toString('x'));
    assertEquals("", Convert.toString((Object) null));
    assertEquals("2.5", Convert.toString((Object) Float.valueOf(2.5f)));
  }
}
