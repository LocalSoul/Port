package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.PortException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

/**
 * Ein Unterbefehl von {@code port}: beschreibt seine Argumente (picocli-Modell, ohne Annotationen)
 * und ruft einen Dienst aus {@code core} auf.
 */
interface PortCommand {

  /**
   * Name des Befehls.
   *
   * @return z. B. {@code init}
   */
  String name();

  /**
   * Beschreibt Argumente und Optionen.
   *
   * @return Modell des Unterbefehls
   */
  CommandSpec spec();

  /**
   * Führt den Befehl aus.
   *
   * @param args gelesene Argumente dieses Unterbefehls
   * @param context globale Einstellungen
   * @return Ergebnis
   * @throws PortException bei Abbruch
   */
  CommandResult run(ParseResult args, CommandContext context) throws PortException;
}
