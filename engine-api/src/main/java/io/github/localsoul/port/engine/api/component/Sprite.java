package io.github.localsoul.port.engine.api.component;

import io.github.localsoul.port.engine.api.world.Entity;

/**
 * 2D-Bild an einer Entity mit {@link Transform} (Schema v1). Die Größe in Welteinheiten ist
 * Texturgröße durch {@code pixelsPerUnit}.
 *
 * @param texture Entity mit der {@link Texture}
 * @param pivotX Drehpunkt horizontal, 0 = links, 1 = rechts
 * @param pivotY Drehpunkt vertikal, 0 = unten, 1 = oben
 * @param pixelsPerUnit Texturpixel je Welteinheit
 * @param order Zeichenreihenfolge, größere Werte später (oben)
 * @param tint Farbe, mit der multipliziert wird (ARGB, {@code 0xFFFFFFFF} = unverändert)
 * @param visible sichtbar
 */
public record Sprite(
    Entity texture,
    float pivotX,
    float pivotY,
    float pixelsPerUnit,
    int order,
    int tint,
    boolean visible) {

  /** Version des Schemas. */
  public static final int SCHEMA_VERSION = 1;
}
