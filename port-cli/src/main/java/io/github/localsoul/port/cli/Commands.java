package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.Phase;
import java.util.List;

/** Alle Befehle der CLI in fester Reihenfolge. */
final class Commands {

  private Commands() {}

  static List<PortCommand> all() {
    List<PortCommand> commands = new java.util.ArrayList<>();
    commands.addAll(
        List.of(
            new InitCommand(),
            new LifecycleCommand(Phase.EXTRACT, "Eingabe prüfen (Existenz, SHA-256)."),
            new LifecycleCommand(Phase.IR, "IR mit dem Frontend erzeugen (ir/)."),
            new LifecycleCommand(Phase.INDEX, "Index über ir/ aktualisieren (.port/index.db)."),
            new LifecycleCommand(Phase.GEN, "Java aus der IR erzeugen (gen/)."),
            new LifecycleCommand(Phase.BUILD, "gen/ und manual/ kompilieren (.port/classes)."),
            new StatusCommand(),
            new CatalogListCommand(),
            new CatalogShowCommand()));
    commands.addAll(LaunchCommands.all());
    commands.addAll(SearchCommands.all());
    return commands;
  }
}
