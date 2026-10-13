package io.github.localsoul.port.core;

import io.github.localsoul.port.json.JsonObject;
import java.util.List;

/** Eine Phase der Pipeline. */
interface PhaseStep {

  /**
   * Führt die Phase aus.
   *
   * @param project Projekt
   * @param options Optionen des Aufrufs
   * @param context globale Einstellungen
   * @param diagnostics sammelt Warnungen und Hinweise
   * @return Daten der Phase
   * @throws PortException bei Abbruch
   */
  JsonObject run(
      Project project,
      PipelineOptions options,
      CommandContext context,
      List<Diagnostic> diagnostics)
      throws PortException;
}
