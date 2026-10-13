package io.github.localsoul.port.engine.core.world;

import java.util.ArrayList;
import java.util.List;

/** Sammelt Strukturänderungen einer Phase in Aufrufreihenfolge. */
final class CommandBuffer {

  private final List<Command> commands = new ArrayList<>();

  void add(Command command) {
    commands.add(command);
  }

  boolean isEmpty() {
    return commands.isEmpty();
  }

  /** Anstehende Befehle in Aufrufreihenfolge, ohne den Puffer zu leeren. */
  List<Command> pending() {
    return List.copyOf(commands);
  }

  /** Liefert alle Befehle und leert den Puffer. */
  List<Command> drain() {
    List<Command> drained = new ArrayList<>(commands);
    commands.clear();
    return drained;
  }
}
