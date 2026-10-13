package io.github.localsoul.port.engine.api.component;

import io.github.localsoul.port.engine.api.world.Entity;

/**
 * Elternobjekt einer Entity. Vom Core verwaltet, nur über {@code World.setParent} änderbar.
 *
 * @param parent Elternobjekt
 */
public record Parent(Entity parent) {

  /** Schemaversion. */
  public static final int SCHEMA_VERSION = 1;
}
