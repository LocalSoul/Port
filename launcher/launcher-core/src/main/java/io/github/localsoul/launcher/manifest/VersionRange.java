package io.github.localsoul.launcher.manifest;

import java.util.ArrayList;
import java.util.List;

/**
 * Versionsbereich wie {@code >=2022.3} oder {@code >=0.1 <0.2}: Bedingungen mit {@code >=}, {@code
 * >}, {@code <=}, {@code <}, {@code =} (oder ohne Operator: gleich), durch Leerzeichen getrennt und
 * alle erfüllt; {@code *} erlaubt jede Version. Versionen bestehen aus Ziffern mit Punkten und
 * werden numerisch je Teil verglichen, fehlende Teile zählen als 0.
 */
public final class VersionRange {

  private record Condition(String operator, String version) {}

  private final String text;
  private final List<Condition> conditions;

  private VersionRange(String text, List<Condition> conditions) {
    this.text = text;
    this.conditions = List.copyOf(conditions);
  }

  /**
   * Liest einen Bereich.
   *
   * @param text z. B. {@code >=0.1 <0.2}
   * @return Bereich
   * @throws IllegalArgumentException bei ungültigem Text
   */
  public static VersionRange parse(String text) {
    String trimmed = text.strip();
    if (trimmed.isEmpty()) {
      throw new IllegalArgumentException("leerer Versionsbereich");
    }
    List<Condition> conditions = new ArrayList<>();
    if (!trimmed.equals("*")) {
      for (String part : trimmed.split("\\s+")) {
        conditions.add(condition(part));
      }
    }
    return new VersionRange(trimmed, conditions);
  }

  private static Condition condition(String part) {
    String operator = "=";
    for (String candidate : new String[] {">=", "<=", ">", "<", "="}) {
      if (part.startsWith(candidate)) {
        operator = candidate;
        break;
      }
    }
    String version = part.startsWith(operator) ? part.substring(operator.length()) : part;
    if (!isVersion(version)) {
      throw new IllegalArgumentException("ungültiger Versionsbereich: '" + part + "'");
    }
    return new Condition(operator, version);
  }

  /**
   * Prüft, ob ein Text eine Version ist.
   *
   * @param text z. B. {@code 2022.3}
   * @return {@code true} für Ziffern mit Punkten
   */
  public static boolean isVersion(String text) {
    return text.matches("\\d+(\\.\\d+)*");
  }

  /**
   * Prüft eine Version.
   *
   * @param version z. B. {@code 0.1}
   * @return {@code true}, wenn alle Bedingungen erfüllt sind
   * @throws IllegalArgumentException wenn {@code version} keine Version ist
   */
  public boolean contains(String version) {
    if (!isVersion(version)) {
      throw new IllegalArgumentException("keine Version: '" + version + "'");
    }
    for (Condition condition : conditions) {
      int order = compare(version, condition.version());
      boolean ok =
          switch (condition.operator()) {
            case ">=" -> order >= 0;
            case ">" -> order > 0;
            case "<=" -> order <= 0;
            case "<" -> order < 0;
            default -> order == 0;
          };
      if (!ok) {
        return false;
      }
    }
    return true;
  }

  /**
   * Vergleicht zwei Versionen numerisch je Teil.
   *
   * @param a erste Version
   * @param b zweite Version
   * @return negativ, 0 oder positiv
   */
  public static int compare(String a, String b) {
    String[] left = a.split("\\.");
    String[] right = b.split("\\.");
    for (int i = 0; i < java.lang.Math.max(left.length, right.length); i++) {
      long l = i < left.length ? Long.parseLong(left[i]) : 0;
      long r = i < right.length ? Long.parseLong(right[i]) : 0;
      if (l != r) {
        return Long.compare(l, r);
      }
    }
    return 0;
  }

  @Override
  public String toString() {
    return text;
  }
}
