package io.github.localsoul.port.engine.api.world;

import java.util.List;

/**
 * Lesbare Momentaufnahme einer Entity für Debugging.
 *
 * @param entity die Entity
 * @param components alle Komponenten, sortiert nach vollem Klassennamen (unveränderlich)
 */
public record EntityView(Entity entity, List<Object> components) {

  /** Kopiert die Komponenten in eine unveränderliche Liste. */
  public EntityView {
    components = List.copyOf(components);
  }
}
