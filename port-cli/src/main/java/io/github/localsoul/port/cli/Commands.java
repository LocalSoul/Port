package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.Phase;
import java.util.List;

/** Alle Befehle der CLI in fester Reihenfolge. */
final class Commands {

  private Commands() {}

  static List<PortCommand> all() {
    return List.of(
        new InitCommand(),
        new LifecycleCommand(Phase.EXTRACT, "Eingabe prüfen (Existenz, SHA-256)."),
        new LifecycleCommand(Phase.IR, "IR mit dem Frontend erzeugen (ir/)."));
  }
}
