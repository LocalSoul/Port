package io.github.localsoul.port.core;

import java.io.PrintStream;
import java.nio.file.Path;

/**
 * Globale Einstellungen eines Aufrufs.
 *
 * @param workingDir Arbeitsverzeichnis (Basis für relative Pfadargumente)
 * @param projectDir Projektordner ({@code -C}, sonst Arbeitsverzeichnis), absolut und normalisiert
 * @param strict {@code --strict}
 * @param verbose {@code -v}
 * @param quiet {@code -q}
 * @param offline {@code --offline} (in M4 ohne Wirkung)
 * @param log Fortschrittsausgabe (stderr)
 * @param platformHome Plattformwurzel außerhalb des Monorepos ({@code PORT_HOME}) oder {@code null}
 * @param out stdout (für Ausgaben gestarteter Spiele ohne {@code --json})
 * @param json {@code --json}: stdout gehört allein der JSON-Hülle
 */
public record CommandContext(
    Path workingDir,
    Path projectDir,
    boolean strict,
    boolean verbose,
    boolean quiet,
    boolean offline,
    PrintStream log,
    Path platformHome,
    PrintStream out,
    boolean json) {

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
