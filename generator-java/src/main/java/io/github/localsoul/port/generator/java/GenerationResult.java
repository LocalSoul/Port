package io.github.localsoul.port.generator.java;

import java.util.List;

/**
 * Ergebnis einer Generierung.
 *
 * @param files erzeugte Dateien, sortiert nach Pfad (unveränderlich)
 * @param todos offene Stellen, sortiert nach Symbol und Grund (unveränderlich)
 */
public record GenerationResult(List<GeneratedFile> files, List<Todo> todos) {

  /** Kopiert die Listen. */
  public GenerationResult {
    files = List.copyOf(files);
    todos = List.copyOf(todos);
  }
}
