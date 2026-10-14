package io.github.localsoul.port.core;

import io.github.localsoul.launcher.LauncherException;

/** Übersetzt Fehler aus {@code launcher-core} in die Ausgabe der CLI. */
public final class LauncherErrors {

  private LauncherErrors() {}

  /**
   * Übersetzt einen Fehler.
   *
   * @param e Fehler aus {@code launcher-core}
   * @return Abbruch mit gleichem Exit-Code und gleicher Diagnose
   */
  public static PortException toPort(LauncherException e) {
    return new PortException(
        ExitCode.of(e.exitCode()),
        Diagnostic.error(e.id(), e.getMessage(), e.location(), e.hint()));
  }
}
