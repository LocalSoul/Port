package io.github.localsoul.port.lint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Konfiguration aus {@code [lint]} in {@code port.toml}: abgeschaltete Regeln und geänderte Stufen.
 *
 * @param disable abgeschaltete Regeln
 * @param severity Stufe je Regel
 */
public record LintConfig(Set<String> disable, Map<String, Severity> severity) {

  /** Keine Änderungen. */
  public static final LintConfig DEFAULT = new LintConfig(Set.of(), Map.of());

  /** Kopiert sortiert. */
  public LintConfig {
    disable = new TreeSet<>(disable);
    severity = new TreeMap<>(severity);
  }

  /**
   * Wendet die Konfiguration an.
   *
   * @param findings Befunde mit Standardstufe
   * @return Befunde ohne abgeschaltete Regeln, mit geänderten Stufen
   */
  public List<LintFinding> apply(List<LintFinding> findings) {
    List<LintFinding> result = new java.util.ArrayList<>();
    for (LintFinding finding : findings) {
      if (disable.contains(finding.id())) {
        continue;
      }
      Severity changed = severity.get(finding.id());
      result.add(changed == null ? finding : finding.withSeverity(changed));
    }
    return result;
  }
}
