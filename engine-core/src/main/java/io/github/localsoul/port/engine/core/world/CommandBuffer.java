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
  /**
   * Ersetzt die Komponente der letzten ausstehenden Hinzufügung dieses Typs an der Entity, wenn
   * danach kein Entfernen oder Zerstören folgt.
   *
   * @return {@code true}, wenn ersetzt wurde
   */
  boolean replacePendingAdd(
      io.github.localsoul.port.engine.api.world.Entity entity, Object component) {
    for (int i = commands.size() - 1; i >= 0; i--) {
      Command command = commands.get(i);
      if (command instanceof Command.Add add
          && add.entity().equals(entity)
          && add.component().getClass() == component.getClass()) {
        commands.set(i, new Command.Add(entity, component));
        return true;
      }
      boolean removed =
          (command instanceof Command.Remove remove
                  && remove.entity().equals(entity)
                  && remove.type() == component.getClass())
              || (command instanceof Command.Destroy destroy && destroy.entity().equals(entity));
      if (removed) {
        return false;
      }
    }
    return false;
  }

  List<Command> drain() {
    List<Command> drained = new ArrayList<>(commands);
    commands.clear();
    return drained;
  }
}
