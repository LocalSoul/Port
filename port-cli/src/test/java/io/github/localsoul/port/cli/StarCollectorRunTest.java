package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * StarCollector Ende zu Ende über {@code port launch run}: IR des Spiels → Java → Build → {@code
 * launcher-boot} mit Offscreen-Ausgabe, festem Startwert und Tastenskript. Geprüft wird an Bildern:
 * Spieler in der Mitte, Sterne erscheinen, ein eingesammelter Stern ändert die Anzeige, nach 30
 * Sekunden kommt das Ende-Fenster, R startet neu. Schreibt nur in die ignorierten Ordner {@code
 * games/starcollector/gen} und {@code .port}.
 */
class StarCollectorRunTest {

  private static final Path GAME = Golden.ROOT.resolve("games/starcollector");

  /** Hintergrund der Unity-Standardkamera (Adapter). */
  private static final int BACKGROUND = 0xFF314D79;

  private static final int CENTER_X = 480;
  private static final int CENTER_Y = 300;

  /** Punkt im Ende-Fenster ohne Text. */
  private static final int PANEL_X = 260;

  private static final int PANEL_Y = 190;

  @BeforeEach
  void requireSubmodules() {
    assumeTrue(
        Files.isRegularFile(GAME.resolve("port.toml")),
        "Submodule nicht ausgecheckt: git submodule update --init");
  }

  @Test
  void playsCollectsTimesOutAndRestarts(@TempDir Path temp) throws IOException {
    Path start = temp.resolve("start.png");
    Path collected = temp.resolve("collected.png");
    Path over = temp.resolve("over.png");
    Path restarted = temp.resolve("restarted.png");
    Cli.Run run =
        Cli.run(
            Commands.all(),
            null,
            Golden.ROOT,
            "launch",
            "run",
            "starcollector-unity",
            "--headless",
            "--",
            "--render",
            "offscreen",
            "--frames",
            "1900",
            "--seed",
            "1",
            "--press",
            "30:RIGHT",
            "--release",
            "90:RIGHT",
            "--press",
            "1850:R",
            "--release",
            "1852:R",
            "--screenshot",
            "5:" + start,
            "--screenshot",
            "120:" + collected,
            "--screenshot",
            "1845:" + over,
            "--screenshot",
            "1900:" + restarted);
    assertEquals(0, run.exitCode(), run.out() + run.err());
    assertTrue(run.out().contains("frames: 1900"), run.out());

    BufferedImage first = ImageIO.read(start.toFile());
    assertEquals(960, first.getWidth());
    int player = first.getRGB(CENTER_X, CENTER_Y);
    assertNotEquals(BACKGROUND, player, "Spieler in der Mitte");
    assertEquals(BACKGROUND, first.getRGB(PANEL_X, PANEL_Y));
    assertTrue(count(first, StarCollectorRunTest::isWhite) > 50, "Anzeige sichtbar");

    BufferedImage afterCollect = ImageIO.read(collected.toFile());
    assertEquals(BACKGROUND, afterCollect.getRGB(CENTER_X, CENTER_Y), "Spieler hat sich bewegt");
    assertTrue(count(afterCollect, StarCollectorRunTest::isYellow) > 50, "Sterne sichtbar");
    assertFalse(sameScoreLabel(first, afterCollect), "Sternzähler hat sich geändert");

    BufferedImage gameOver = ImageIO.read(over.toFile());
    int panel = gameOver.getRGB(PANEL_X, PANEL_Y);
    assertTrue(isDark(panel), Integer.toHexString(panel));

    BufferedImage again = ImageIO.read(restarted.toFile());
    assertEquals(player, again.getRGB(CENTER_X, CENTER_Y), "Spieler wieder in der Mitte");
    assertEquals(BACKGROUND, again.getRGB(PANEL_X, PANEL_Y), "Ende-Fenster weg");
    assertTrue(sameScoreLabel(first, again), "Sternzähler wieder 0");
  }

  /** Bereich der Anzeige „Sterne: n“ oben links. */
  private static boolean sameScoreLabel(BufferedImage a, BufferedImage b) {
    for (int y = 15; y < 55; y++) {
      for (int x = 15; x < 170; x++) {
        if (a.getRGB(x, y) != b.getRGB(x, y)) {
          return false;
        }
      }
    }
    return true;
  }

  private interface PixelTest {
    boolean matches(int argb);
  }

  private static int count(BufferedImage image, PixelTest test) {
    int result = 0;
    for (int y = 0; y < image.getHeight(); y++) {
      for (int x = 0; x < image.getWidth(); x++) {
        if (test.matches(image.getRGB(x, y))) {
          result++;
        }
      }
    }
    return result;
  }

  private static boolean isYellow(int argb) {
    return red(argb) > 0xD0 && green(argb) > 0xB0 && blue(argb) < 0x80;
  }

  private static boolean isWhite(int argb) {
    return red(argb) > 0xF0 && green(argb) > 0xF0 && blue(argb) > 0xF0;
  }

  private static boolean isDark(int argb) {
    return red(argb) < 0x40 && green(argb) < 0x40 && blue(argb) < 0x50;
  }

  private static int red(int argb) {
    return (argb >> 16) & 0xFF;
  }

  private static int green(int argb) {
    return (argb >> 8) & 0xFF;
  }

  private static int blue(int argb) {
    return argb & 0xFF;
  }
}
