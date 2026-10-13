package io.github.localsoul.port.ir;

import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/** Platzhaltertest für M0.1, damit der Build Tests ausführt. Wird ab M0.2 ersetzt. */
class SkeletonTest {

  @Test
  void runsOnJava25OrNewer() {
    int feature = Runtime.version().feature();
    boolean supported = feature >= 25;
    assertTrue(supported, "Java-Version " + feature + " ist kleiner als 25");
  }
}
