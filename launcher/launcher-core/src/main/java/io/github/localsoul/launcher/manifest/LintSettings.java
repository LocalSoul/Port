package io.github.localsoul.launcher.manifest;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abschnitt {@code [lint]} eines Spiel-Manifests ({@code docs/cli-contract.md}).
 *
 * @param disable abgeschaltete Regeln, z. B. {@code SM007}
 * @param severity geänderte Stufen je Regel ({@code error}, {@code warn}, {@code info}), sortiert
 */
public record LintSettings(List<String> disable, Map<String, String> severity) {

  /** Keine Einstellungen. */
  public static final LintSettings NONE = new LintSettings(List.of(), Map.of());

  /** Kopiert und sortiert. */
  public LintSettings {
    disable = List.copyOf(disable);
    severity = new TreeMap<>(severity);
  }

  /**
   * {@code true}, wenn nichts eingestellt ist.
   *
   * @return keine Einstellung
   */
  public boolean isEmpty() {
    return disable.isEmpty() && severity.isEmpty();
  }
}
