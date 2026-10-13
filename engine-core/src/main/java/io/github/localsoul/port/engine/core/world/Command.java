package io.github.localsoul.port.engine.core.world;

import io.github.localsoul.port.engine.api.world.Entity;

/** Aufgeschobene Strukturänderung im {@link CommandBuffer}. */
sealed interface Command {

  /**
   * Entity zerstören.
   *
   * @param entity Entity
   */
  record Destroy(Entity entity) implements Command {}

  /**
   * Komponente anhängen.
   *
   * @param entity Entity
   * @param component Komponente
   */
  record Add(Entity entity, Object component) implements Command {}

  /**
   * Komponente entfernen.
   *
   * @param entity Entity
   * @param type Typ
   */
  record Remove(Entity entity, Class<?> type) implements Command {}

  /**
   * Elternobjekt setzen oder lösen.
   *
   * @param child Kind
   * @param parent Elternobjekt oder {@code null}
   */
  record SetParent(Entity child, Entity parent) implements Command {}
}
