package io.github.localsoul.port.render.swing;

import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.render.Affine2D;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Zeichenlogik mit Java2D auf ein {@link BufferedImage}, gemeinsam für Fenster und Offscreen.
 * Texturen werden je Instanz (und Tönung) einmal in Bilder umgewandelt und zwischengespeichert.
 */
final class Java2DCanvas {

  private final BufferedImage image;
  private final Map<Texture, Map<Integer, BufferedImage>> textures = new WeakHashMap<>();
  private Graphics2D graphics;

  Java2DCanvas(int width, int height) {
    this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
  }

  BufferedImage image() {
    return image;
  }

  void begin(int background) {
    if (graphics != null) {
      graphics.dispose();
    }
    graphics = image.createGraphics();
    graphics.setComposite(AlphaComposite.Src);
    graphics.setColor(new Color(background | 0xFF000000, true));
    graphics.fillRect(0, 0, image.getWidth(), image.getHeight());
    graphics.setComposite(AlphaComposite.SrcOver);
    graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    graphics.setRenderingHint(
        RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
  }

  void drawTexture(Texture texture, Affine2D t, int tint) {
    AffineTransform saved = graphics.getTransform();
    graphics.setRenderingHint(
        RenderingHints.KEY_INTERPOLATION,
        texture.smooth()
            ? RenderingHints.VALUE_INTERPOLATION_BILINEAR
            : RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    graphics.setTransform(
        new AffineTransform(t.m00(), t.m10(), t.m01(), t.m11(), t.m02(), t.m12()));
    graphics.drawImage(imageOf(texture, tint), 0, 0, null);
    graphics.setTransform(saved);
  }

  void fillRect(float x, float y, float width, float height, int fill, int border) {
    graphics.setColor(new Color(fill, true));
    graphics.fill(new Rectangle2D.Float(x, y, width, height));
    if ((border >>> 24) != 0) {
      graphics.setColor(new Color(border, true));
      graphics.setStroke(new BasicStroke(1f));
      graphics.draw(new Rectangle2D.Float(x + 0.5f, y + 0.5f, width - 1f, height - 1f));
    }
  }

  void drawText(
      String text,
      float x,
      float y,
      float width,
      float height,
      int size,
      boolean bold,
      int anchor,
      int color) {
    graphics.setFont(new Font(Font.SANS_SERIF, bold ? Font.BOLD : Font.PLAIN, Math.max(1, size)));
    graphics.setColor(new Color(color, true));
    FontMetrics metrics = graphics.getFontMetrics();
    int column = Math.floorMod(anchor, 3);
    int row = Math.floorDiv(Math.max(0, anchor), 3);
    float textWidth = metrics.stringWidth(text);
    float drawX =
        switch (column) {
          case 0 -> x;
          case 1 -> x + (width - textWidth) / 2f;
          default -> x + width - textWidth;
        };
    float textHeight = metrics.getAscent() + metrics.getDescent();
    float top =
        switch (row) {
          case 0 -> y;
          case 1 -> y + (height - textHeight) / 2f;
          default -> y + height - textHeight;
        };
    graphics.drawString(text, drawX, top + metrics.getAscent());
  }

  void finish() {
    if (graphics != null) {
      graphics.dispose();
      graphics = null;
    }
  }

  private BufferedImage imageOf(Texture texture, int tint) {
    Map<Integer, BufferedImage> byTint =
        textures.computeIfAbsent(texture, key -> new java.util.HashMap<>());
    BufferedImage cached = byTint.get(tint);
    if (cached == null) {
      cached = convert(texture, tint);
      byTint.put(tint, cached);
    }
    return cached;
  }

  /** ARGB-Pixel (Zeile 0 oben) → Bild, mit Tönung multipliziert. */
  static BufferedImage convert(Texture texture, int tint) {
    BufferedImage result =
        new BufferedImage(texture.width(), texture.height(), BufferedImage.TYPE_INT_ARGB);
    int[] pixels = texture.pixels();
    int[] tinted = new int[pixels.length];
    for (int i = 0; i < pixels.length; i++) {
      tinted[i] = tint == 0xFFFFFFFF ? pixels[i] : multiply(pixels[i], tint);
    }
    result.setRGB(0, 0, texture.width(), texture.height(), tinted, 0, texture.width());
    return result;
  }

  private static int multiply(int a, int b) {
    int alpha = ((a >>> 24) * (b >>> 24)) / 255;
    int red = (((a >> 16) & 0xFF) * ((b >> 16) & 0xFF)) / 255;
    int green = (((a >> 8) & 0xFF) * ((b >> 8) & 0xFF)) / 255;
    int blue = ((a & 0xFF) * (b & 0xFF)) / 255;
    return (alpha << 24) | (red << 16) | (green << 8) | blue;
  }
}
