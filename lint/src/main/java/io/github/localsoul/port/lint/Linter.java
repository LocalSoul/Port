package io.github.localsoul.port.lint;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Führt alle Regeln aus ({@code docs/modules/lint.md}): IR001–IR005, PT001–PT003, SM001,
 * SH001/SH002, MN001–MN004. Ergebnis nach Konfiguration, sortiert nach Ort, Regel und Meldung.
 */
public final class Linter {

  private static final List<Rule> RULES =
      List.of(
          new ValidatorRule(),
          new PortabilityRule(),
          new StructCopyRule(),
          new ShimRule(),
          new ManualRule());

  private Linter() {}

  /**
   * Prüft ein Projekt.
   *
   * @param input IR, Markierungen und Abdeckung
   * @param config Konfiguration aus {@code port.toml}
   * @return Befunde
   */
  public static List<LintFinding> lint(LintInput input, LintConfig config) {
    ProjectModel model = ProjectModel.of(input);
    List<LintFinding> findings = new ArrayList<>();
    for (Rule rule : RULES) {
      rule.check(input, model, findings);
    }
    List<LintFinding> result = new ArrayList<>(config.apply(findings));
    result.sort(
        Comparator.comparing((LintFinding f) -> f.location() == null ? "" : f.location())
            .thenComparing(LintFinding::id)
            .thenComparing(LintFinding::message));
    return result;
  }
}
