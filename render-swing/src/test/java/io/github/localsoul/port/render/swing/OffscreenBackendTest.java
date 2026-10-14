package io.github.localsoul.port.render.swing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.render.Affine2D;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Pixelprüfungen im Offscreen-Modus (headless). */
class OffscreenBackendTest {

  private static final int RED = 0xFFFF0000;
  private static final int BLUE = 0xFF0000FF;

  private static Texture texture(int... pixels) {
    return new Texture(2, 2, pixels, false);
  }

  @Test
  void drawsScaledTextureWithTransform() {
    OffscreenBackend backend = new OffscreenBackend(40, 30);
    backend.begin(0xFF000000);
    // 2x2-Textur, jeder Texel 10x10 Pixel, oben links bei (5, 5); unten rechts blau.
    backend.drawTexture(texture(RED, RED, RED, BLUE), new Affine2D(10, 0, 5, 0, 10, 5), 0xFFFFFFFF);
    backend.present();
    BufferedImage image = backend.image();
    assertEquals(0xFF000000, image.getRGB(2, 2));
    assertEquals(RED, image.getRGB(8, 8));
    assertEquals(BLUE, image.getRGB(20, 20));
    assertEquals(1, backend.frames());
  }

  @Test
  void tintMultipliesColors() {
    OffscreenBackend backend = new OffscreenBackend(10, 10);
    backend.begin(0xFF000000);
    Texture white = new Texture(1, 1, new int[] {0xFFFFFFFF}, false);
    backend.drawTexture(white, new Affine2D(10, 0, 0, 0, 10, 0), 0xFF00FF00);
    backend.present();
    assertEquals(0xFF00FF00, backend.image().getRGB(5, 5));
  }

  @Test
  void rectanglesAndText() {
    OffscreenBackend backend = new OffscreenBackend(200, 60);
    backend.begin(0xFF000000);
    backend.fillRect(0, 0, 50, 50, 0xFF404040, 0xFFFFFFFF);
    backend.drawText("Sterne: 3", 60, 0, 140, 60, 28, true, 4, 0xFFFFFFFF);
    backend.present();
    BufferedImage image = backend.image();
    assertEquals(0xFF404040, image.getRGB(25, 25));
    assertEquals(0xFFFFFFFF, image.getRGB(0, 25));
    boolean textPixel = false;
    for (int x = 60; x < 200 && !textPixel; x++) {
      for (int y = 0; y < 60 && !textPixel; y++) {
        textPixel = (image.getRGB(x, y) & 0xFF) > 0x80;
      }
    }
    assertTrue(textPixel, "Text sichtbar");
  }

  @Test
  void writesPng(@TempDir Path temp) throws Exception {
    OffscreenBackend backend = new OffscreenBackend(4, 4);
    backend.begin(0xFF102030);
    backend.present();
    Path file = temp.resolve("shots/frame.png");
    backend.writePng(file);
    assertTrue(Files.isRegularFile(file));
    assertEquals(0xFF102030, ImageIO.read(file.toFile()).getRGB(1, 1));
  }

  @Test
  void keyNamesAreNeutral() {
    assertEquals("A", KeyNames.of(java.awt.event.KeyEvent.VK_A));
    assertEquals("7", KeyNames.of(java.awt.event.KeyEvent.VK_7));
    assertEquals("ESCAPE", KeyNames.of(java.awt.event.KeyEvent.VK_ESCAPE));
    assertEquals("LEFT", KeyNames.of(java.awt.event.KeyEvent.VK_LEFT));
    assertNull(KeyNames.of(java.awt.event.KeyEvent.VK_F1));
  }
}
