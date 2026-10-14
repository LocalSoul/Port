package io.github.localsoul.port.engine.api.component;

/**
 * Orthografische 2D-Kamera an einer Entity mit {@link Transform} (Schema v1): Mittelpunkt ist die
 * Position, sichtbar ist die halbe Höhe {@code orthographicSize} in Welteinheiten; die Breite folgt
 * aus dem Seitenverhältnis des {@link Viewport}.
 *
 * @param orthographicSize halbe sichtbare Höhe in Welteinheiten
 * @param background Hintergrundfarbe (ARGB)
 */
public record Camera2D(float orthographicSize, int background) {

  /** Version des Schemas. */
  public static final int SCHEMA_VERSION = 1;
}
