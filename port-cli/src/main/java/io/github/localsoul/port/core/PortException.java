package io.github.localsoul.port.core;

/** Abbruch eines Befehls mit Exit-Code und Diagnose. */
public final class PortException extends Exception {

  private static final long serialVersionUID = 1L;

  private final transient ExitCode exitCode;
  private final transient Diagnostic diagnostic;

  /**
   * Erzeugt den Abbruch.
   *
   * @param exitCode Exit-Code laut Vertrag
   * @param diagnostic Fehlerdiagnose
   */
  public PortException(ExitCode exitCode, Diagnostic diagnostic) {
    super(diagnostic.message());
    this.exitCode = exitCode;
    this.diagnostic = diagnostic;
  }

  /**
   * Exit-Code laut Vertrag.
   *
   * @return Exit-Code
   */
  public ExitCode exitCode() {
    return exitCode;
  }

  /**
   * Fehlerdiagnose.
   *
   * @return Diagnose
   */
  public Diagnostic diagnostic() {
    return diagnostic;
  }
}
