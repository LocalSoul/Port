package io.github.localsoul.port.core;

import io.github.localsoul.port.index.IndexException;

/** Übersetzt Fehler des Index in die Ausgabe der CLI. */
final class IndexErrors {

  private IndexErrors() {}

  static PortException toPort(IndexException e) {
    String id = e.exitCode() == IndexException.INVALID ? DiagnosticIds.IR : DiagnosticIds.INDEX;
    String hint =
        e.exitCode() == IndexException.INVALID ? "port ir neu ausführen" : "port index --rebuild";
    return new PortException(
        ExitCode.of(e.exitCode()), Diagnostic.error(id, e.getMessage(), e.location(), hint));
  }
}
