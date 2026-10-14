package io.github.localsoul.port.render.swing;

import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.input.KeyEvent;
import io.github.localsoul.port.engine.api.render.Affine2D;
import io.github.localsoul.port.engine.api.render.RenderBackend;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;

/**
 * Backend ohne Fenster (ADR-014): zeichnet mit Java2D in ein Bild, läuft headless. Für Tests und
 * Screenshots; Tastatur gibt es keine.
 */
public final class OffscreenBackend implements RenderBackend {

  private final Java2DCanvas canvas;
  private long frames;

  /**
   * Erzeugt das Backend.
   *
   * @param width Breite in Pixeln
   * @param height Höhe in Pixeln
   */
  public OffscreenBackend(int width, int height) {
    this.canvas = new Java2DCanvas(width, height);
  }

  /**
   * Das zuletzt gezeichnete Bild.
   *
   * @return Bild (wird beim nächsten Frame überschrieben)
   */
  public BufferedImage image() {
    return canvas.image();
  }

  /**
   * Anzahl der fertigen Frames.
   *
   * @return Frames
   */
  public long frames() {
    return frames;
  }

  /**
   * Schreibt das aktuelle Bild als PNG.
   *
   * @param file Ziel
   * @throws IOException bei Schreibfehlern
   */
  public void writePng(Path file) throws IOException {
    if (file.toAbsolutePath().getParent() != null) {
      Files.createDirectories(file.toAbsolutePath().getParent());
    }
    ImageIO.write(canvas.image(), "png", file.toFile());
  }

  @Override
  public int width() {
    return canvas.image().getWidth();
  }

  @Override
  public int height() {
    return canvas.image().getHeight();
  }

  @Override
  public void begin(int background) {
    canvas.begin(background);
  }

  @Override
  public void drawTexture(Texture texture, Affine2D transform, int tint) {
    canvas.drawTexture(texture, transform, tint);
  }

  @Override
  public void fillRect(float x, float y, float width, float height, int fill, int border) {
    canvas.fillRect(x, y, width, height, fill, border);
  }

  @Override
  public void drawText(
      String text,
      float x,
      float y,
      float width,
      float height,
      int size,
      boolean bold,
      int anchor,
      int color) {
    canvas.drawText(text, x, y, width, height, size, bold, anchor, color);
  }

  @Override
  public void present() {
    canvas.finish();
    frames++;
  }

  @Override
  public List<KeyEvent> pollKeys() {
    return List.of();
  }

  @Override
  public boolean closed() {
    return false;
  }

  @Override
  public void close() {
    canvas.finish();
  }
}
