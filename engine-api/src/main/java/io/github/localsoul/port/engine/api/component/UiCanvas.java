package io.github.localsoul.port.engine.api.component;

import java.util.List;

/**
 * Bildschirm-UI eines Frames (Schema v1): Zeichenbefehle in Pixelkoordinaten (Ursprung oben links),
 * in Listenreihenfolge über der Welt gezeichnet. Der Besitzer ersetzt die Liste je Frame.
 *
 * @param elements Elemente in Zeichenreihenfolge
 */
public record UiCanvas(List<UiCanvas.Element> elements) {

  /** Version des Schemas. */
  public static final int SCHEMA_VERSION = 1;

  /** Leere UI. */
  public static final UiCanvas EMPTY = new UiCanvas(List.of());

  /** Kopiert die Liste. */
  public UiCanvas {
    elements = List.copyOf(elements);
  }

  /** Ein UI-Element. */
  public sealed interface Element permits Text, Box {}

  /**
   * Text in einem Rechteck.
   *
   * @param x links
   * @param y oben
   * @param width Breite
   * @param height Höhe
   * @param text Text (eine Zeile)
   * @param size Schriftgröße in Pixeln
   * @param bold fett
   * @param anchor Ausrichtung 0–8: Zeile (oben, Mitte, unten) × 3 + Spalte (links, Mitte, rechts)
   * @param color Farbe (ARGB)
   */
  public record Text(
      float x,
      float y,
      float width,
      float height,
      String text,
      int size,
      boolean bold,
      int anchor,
      int color)
      implements Element {}

  /**
   * Gefülltes Rechteck mit Rahmen.
   *
   * @param x links
   * @param y oben
   * @param width Breite
   * @param height Höhe
   * @param fill Füllfarbe (ARGB)
   * @param border Rahmenfarbe (ARGB, Alpha 0 = kein Rahmen)
   */
  public record Box(float x, float y, float width, float height, int fill, int border)
      implements Element {}
}
