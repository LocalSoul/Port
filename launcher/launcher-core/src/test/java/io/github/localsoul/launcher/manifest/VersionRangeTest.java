package io.github.localsoul.launcher.manifest;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class VersionRangeTest {

  @Test
  void comparesNumericallyPerPart() {
    assertTrue(VersionRange.parse(">=2022.3").contains("2022.10"));
    assertFalse(VersionRange.parse(">=2022.3").contains("2021.9"));
    assertTrue(VersionRange.parse(">=0.1 <0.2").contains("0.1"));
    assertTrue(VersionRange.parse(">=0.1 <0.2").contains("0.1.5"));
    assertFalse(VersionRange.parse(">=0.1 <0.2").contains("0.2"));
    assertTrue(VersionRange.parse("2022.3").contains("2022.3.0"));
    assertTrue(VersionRange.parse("=1").contains("1.0"));
    assertTrue(VersionRange.parse(">1 <=2").contains("2"));
    assertTrue(VersionRange.parse("*").contains("7.7"));
  }

  @Test
  void rejectsInvalidText() {
    assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(""));
    assertThrows(IllegalArgumentException.class, () -> VersionRange.parse(">=x"));
    assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("~1.2"));
    assertThrows(IllegalArgumentException.class, () -> VersionRange.parse("*").contains("neu"));
  }
}
