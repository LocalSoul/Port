package io.github.localsoul.port.engine.api.component;

import java.util.Arrays;

/**
 * Bild im Speicher (Schema v1): Pixel als ARGB, Zeilen von oben nach unten.
 *
 * @param width Breite in Pixeln
 * @param height Höhe in Pixeln
 * @param pixels {@code width * height} ARGB-Werte, Zeile 0 oben (wird nicht kopiert, nach dem
 *     Setzen nicht mehr ändern)
 * @param smooth bilinear filtern beim Skalieren, sonst nächster Nachbar
 */
public record Texture(int width, int height, int[] pixels, boolean smooth) {

  /** Version des Schemas. */
  public static final int SCHEMA_VERSION = 1;

  /**
   * Prüft die Maße.
   *
   * @throws IllegalArgumentException bei Größe ≤ 0 oder falscher Pixelzahl
   */
  public Texture {
    if (width <= 0 || height <= 0 || pixels.length != width * height) {
      throw new IllegalArgumentException(
          "Textur " + width + "x" + height + " mit " + pixels.length + " Pixeln");
    }
  }

  /** Kurzform für den Welt-Dump (die Pixel selbst würden ihn sprengen). */
  @Override
  public String toString() {
    return "Texture["
        + width
        + "x"
        + height
        + ", hash="
        + Integer.toHexString(Arrays.hashCode(pixels))
        + ", smooth="
        + smooth
        + "]";
  }
}
