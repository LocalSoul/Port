package io.github.localsoul.launcher.boot;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.Test;

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
  void errorsHaveDistinctExitCodes() {
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
