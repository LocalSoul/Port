package io.github.localsoul.port.engine.api.render;

import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.input.KeyEvent;
import java.util.List;

/**
 * Zeichenfläche eines Renderers (ADR-014): Fenster, Offscreen-Bild oder nichts. Der Renderer im
 * Core übersetzt die Welt in diese Befehle; Backends sind austauschbar. Koordinaten in Pixeln,
 * Ursprung oben links.
 */
public interface RenderBackend {

  /**
   * Breite der Zeichenfläche.
   *
   * @return Pixel
   */
  int width();

  /**
   * Höhe der Zeichenfläche.
   *
   * @return Pixel
   */
  int height();

  /**
   * Beginnt einen Frame und füllt ihn.
   *
   * @param background Hintergrundfarbe (ARGB)
   */
  void begin(int background);

  /**
   * Zeichnet eine Textur.
   *
   * @param texture Textur
   * @param transform Abbildung von Texturpixeln (Ursprung oben links) auf Bildschirmpixel
   * @param tint Farbe zum Multiplizieren (ARGB, {@code 0xFFFFFFFF} = unverändert)
   */
  void drawTexture(Texture texture, Affine2D transform, int tint);

  /**
   * Füllt ein Rechteck und zeichnet optional einen Rahmen.
   *
   * @param x links
   * @param y oben
   * @param width Breite
   * @param height Höhe
   * @param fill Füllfarbe (ARGB)
   * @param border Rahmenfarbe (ARGB, Alpha 0 = kein Rahmen)
   */
  void fillRect(float x, float y, float width, float height, int fill, int border);

  /**
   * Zeichnet eine Textzeile in ein Rechteck.
   *
   * @param text Text
   * @param x links
   * @param y oben
   * @param width Breite
   * @param height Höhe
   * @param size Schriftgröße in Pixeln
   * @param bold fett
   * @param anchor Ausrichtung 0–8 wie {@link
   *     io.github.localsoul.port.engine.api.component.UiCanvas.Text}
   * @param color Farbe (ARGB)
   */
  void drawText(
      String text,
      float x,
      float y,
      float width,
      float height,
      int size,
      boolean bold,
      int anchor,
      int color);

  /** Zeigt den Frame an (Fenster) bzw. schließt ihn ab (Offscreen). */
  void present();

  /**
   * Seit dem letzten Aufruf eingegangene Tastenereignisse.
   *
   * @return Ereignisse in Eingangsreihenfolge
   */
  List<KeyEvent> pollKeys();

  /**
   * {@code true}, wenn das Fenster geschlossen wurde.
   *
   * @return geschlossen
   */
  boolean closed();

  /** Gibt Fenster und Ressourcen frei. */
  void close();
}
