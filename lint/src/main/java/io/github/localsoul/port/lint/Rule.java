package io.github.localsoul.port.lint;

import java.util.List;

/** Eine Regel oder Regelgruppe; liefert Befunde mit Standardstufe. */
interface Rule {

  /**
   * Prüft das Projekt.
   *
   * @param input Eingabe
   * @param model Projektmodell
   * @param findings sammelt Befunde
   */
  void check(LintInput input, ProjectModel model, List<LintFinding> findings);
}
