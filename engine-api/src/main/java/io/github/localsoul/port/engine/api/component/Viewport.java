package io.github.localsoul.port.engine.api.component;

/**
 * Größe der Zeichenfläche in Pixeln (Schema v1), geschrieben vom Renderer.
 *
 * @param width Breite
 * @param height Höhe
 */
public record Viewport(int width, int height) {

  /** Version des Schemas. */
  public static final int SCHEMA_VERSION = 1;
}
