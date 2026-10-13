package io.github.localsoul.port.core;

/** Zentrale Ableitung des Exit-Codes aus einem Ergebnis ({@code docs/cli-contract.md}). */
public final class ExitCodes {

  private ExitCodes() {}

  /**
   * Leitet den Exit-Code ab: die Fehlerklasse eines abgebrochenen Befehls; sonst mit {@code
   * --strict} {@link ExitCode#MANUAL_OPEN}, wenn offene Handarbeit gemeldet wurde; sonst Erfolg.
   *
   * @param result Ergebnis
   * @param strict {@code --strict} gesetzt
   * @return Exit-Code
   */
  public static ExitCode of(CommandResult result, boolean strict) {
    if (result.failure() != null) {
      return result.failure();
    }
    if (strict) {
      for (Diagnostic diagnostic : result.diagnostics()) {
        if (diagnostic.id().equals(DiagnosticIds.MANUAL)) {
          return ExitCode.MANUAL_OPEN;
        }
      }
    }
    return ExitCode.SUCCESS;
  }
}
