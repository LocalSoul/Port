package io.github.localsoul.port.core;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Globale Einstellungen eines Aufrufs.
 *
 * @param projectDir Projektordner ({@code -C}, sonst Arbeitsverzeichnis), absolut und normalisiert
 * @param strict {@code --strict}
 * @param verbose {@code -v}
 * @param quiet {@code -q}
 * @param offline {@code --offline} (in M4 ohne Wirkung)
 * @param log Fortschrittsausgabe (stderr)
 */
public record CommandContext(
    Path projectDir,
    boolean strict,
    boolean verbose,
    boolean quiet,
    boolean offline,
    PrintStream log) {

  /**
   * Fortschrittsmeldung auf stderr, nur mit {@code -v}.
   *
   * @param message Meldung
   */
  public void progress(String message) {
    if (verbose) {
      log.println(message);
    }
  }
}
