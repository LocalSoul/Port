package io.github.localsoul.launcher.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BootMainTest {

  private record Run(int code, String out, String err) {}

  private static Run run(String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int code =
        BootMain.run(
            args,
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8),
            BootMainTest.class.getClassLoader());
    return new Run(
        code, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  private static final String SCRIPT = TestBinding.Script.class.getName();

  @Test
  void spawnsScriptsRunsFramesAndPrintsDump() {
    Run run = run("--adapter", "test", "--frames", "2", "--axis", "Horizontal=1", "--", SCRIPT);
    assertEquals(0, run.code(), run.err());
    assertTrue(run.out().startsWith("frames: 2\n"), run.out());
    assertEquals(
        List.of("spawn TestBinding$Script", "frame 0 axis 1.0", "frame 1 axis 1.0"),
        TestBinding.LOG);
  }

  @Test
  void scriptedKeysSeedAndQuit() {
    Run run =
        run(
            "--adapter",
            "test",
            "--frames",
            "10",
            "--seed",
            "7",
            "--setting",
            "a.b=#1",
            "--press",
            "1:R",
            "--release",
            "2:R",
            "--press",
            "3:ESCAPE",
            "--",
            SCRIPT);
    assertEquals(0, run.code(), run.err());
    assertTrue(run.out().startsWith("frames: 4\n"), run.out());
    assertEquals(
        List.of(
            "setting seed=7",
            "setting a.b=#1",
            "spawn TestBinding$Script",
            "frame 0 axis 0.0",
            "frame 1 axis 0.0 R",
            "frame 2 axis 0.0",
            "frame 3 axis 0.0"),
        TestBinding.LOG);
  }

  @Test
  void offscreenWritesScreenshots(@TempDir Path temp) throws Exception {
    Path png = temp.resolve("bild.png");
    Run run =
        run(
            "--adapter",
            "test",
            "--frames",
            "3",
            "--render",
            "offscreen",
            "--size",
            "40x30",
            "--screenshot",
            "2:" + png,
            "--screenshot",
            "9:" + temp.resolve("spaet.png"),
            SCRIPT);
    assertEquals(0, run.code(), run.err());
    BufferedImage image = ImageIO.read(png.toFile());
    assertEquals(40, image.getWidth());
    assertEquals(30, image.getHeight());
    assertEquals(TestBinding.RED, image.getRGB(20, 15));
    assertFalse(Files.exists(temp.resolve("spaet.png")));
    assertTrue(run.err().contains("kein Bild nach Frame 9"), run.err());
  }

  @Test
  void realtimeRunsUntilFrameLimit() {
    Run run =
        run(
            "--adapter",
            "test",
            "--frames",
            "3",
            "--realtime",
            "--frame-delta",
            "0.001",
            "--",
            SCRIPT);
    assertEquals(0, run.code(), run.err());
    assertEquals("frames: 3\n", run.out());
  }

  @Test
  void errorsHaveDistinctExitCodes() {
    assertEquals(2, run("--adapter", "test", "--render", "vulkan").code());
    assertEquals(2, run("--adapter", "test", "--screenshot", "1:a.png").code());
    assertEquals(2, run("--adapter", "test", "--render", "window", "--press", "1:R").code());
    assertEquals(2, run("--adapter", "test", "--press", "R").code());
    assertEquals(2, run("--adapter", "test", "--setting", "ohneWert").code());
    assertEquals(2, run("--adapter", "test", "--setting", "kaputt=1").code());
    assertEquals(2, run("--adapter", "test", "--size", "0x10").code());
    assertEquals(2, run("--frames", "1").code());
    assertEquals(2, run("--adapter", "gibtsnicht").code());
    assertEquals(2, run("--adapter", "test", "--axis", "ohneWert").code());
    assertEquals(2, run("--adapter", "test", "--unbekannt").code());
    assertEquals(3, run("--adapter", "test", "gibt.es.Nicht").code());
    Run broken = run("--adapter", "test", TestBinding.Broken.class.getName());
    assertEquals(1, broken.code());
    assertTrue(broken.err().contains("kaputt"), broken.err());
    assertEquals(1, run("--adapter", "test", "java.lang.Object").code());
  }
}
