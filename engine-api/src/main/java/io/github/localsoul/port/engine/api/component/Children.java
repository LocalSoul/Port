package io.github.localsoul.port.engine.api.component;

import io.github.localsoul.port.engine.api.world.Entity;
import java.util.List;

/**
 * Kinder einer Entity in Reihenfolge des Anhängens. Vom Core verwaltet, nur über {@code
 * World.setParent} änderbar.
 *
 * @param children Kinder (unveränderlich)
 */
public record Children(List<Entity> children) {

  /** Schemaversion. */
  public static final int SCHEMA_VERSION = 1;

  /** Kopiert die Kinder in eine unveränderliche Liste. */
  public Children {
    children = List.copyOf(children);
  }
}
