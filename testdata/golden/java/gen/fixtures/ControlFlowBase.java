package gen.fixtures;

import io.github.localsoul.runtime.system.Strings;

/**
 * Generiert aus dem IR-Typ {@code ControlFlow}. Nicht bearbeiten, Handarbeit in der Unterklasse.
 */
public abstract class ControlFlowBase {
  public float average(float[] values) {
    if (values.length == 0) {
      return 0.0f;
    }
    float total = 0.0f;
    for (int i = 0; i < values.length; i++) {
      total += values[i];
    }
    return total / (float) values.length;
  }

  public int countDown(int start) {
    int steps = 0;
    while (start > 0) {
      start--;
      steps++;
      if (steps > 100) {
        break;
      }
    }
    return steps;
  }

  public String describe(int code) {
    switch (code) {
      case 0:
        return "null";
      case 1:
      case 2:
        return "klein";
      default:
        return "groß";
    }
  }

  public int score(String grade) {
    if (!Strings.opEquality(grade, "A")) {
      if (Strings.opEquality(grade, "B")) {
        return 2;
      }
      return 0;
    }
    return 3;
  }

  public int sum(int[] values) {
    int sum = 0;
    for (int v : values) {
      if (v >= 0) {
        sum += v;
      }
    }
    return sum;
  }
}
