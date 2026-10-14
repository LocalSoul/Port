package io.github.localsoul.port.lint;

import java.util.List;

/**
 * Eingabe eines Lint-Laufs.
 *
 * @param sources alle IR-Dateien des Projekts, sortiert nach Pfad
 * @param overrides Markierungen aus {@code manual/}
 * @param coverage Abdeckung von Adapter und {@code runtime-system}
 */
public record LintInput(List<IrSource> sources, List<ManualOverride> overrides, Coverage coverage) {

  /** Kopiert die Listen. */
  public LintInput {
    sources = List.copyOf(sources);
    overrides = List.copyOf(overrides);
  }
}
