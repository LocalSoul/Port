package io.github.localsoul.port.frontend.unity;

import java.time.Duration;
import java.util.List;

/** Startet ein externes Programm; austauschbar für Tests ohne .NET. */
public interface ToolRunner {

  /**
   * Führt das Programm aus und wartet auf das Ende.
   *
   * @param command Programm und Argumente
   * @param timeout maximale Laufzeit
   * @return Exit-Code und stderr
   * @throws FrontendException wenn das Programm nicht startet ({@link FrontendException#GENERAL})
   *     oder die Zeit überschreitet ({@link FrontendException#TRANSLATOR_FAILURE})
   */
  ToolResult run(List<String> command, Duration timeout) throws FrontendException;
}
