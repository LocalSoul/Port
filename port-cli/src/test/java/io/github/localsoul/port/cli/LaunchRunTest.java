package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * {@code port launch run demo-unity} im echten Monorepo: das Testspiel startet headless in eigener
 * JVM, der Mover steht nach 60 Frames bei x = 5 (wie {@code MoverEndToEndTest}). Schreibt nur in
 * die ignorierten Ordner {@code games/demo/gen} und {@code games/demo/.port}.
 */
class LaunchRunTest {

  private static final Path DEMO = Golden.ROOT.resolve("games/demo");

  @BeforeEach
  void requireSubmodules() {
    assumeTrue(
        Files.isRegularFile(DEMO.resolve("port.toml")),
        "Submodule nicht ausgecheckt: git submodule update --init");
  }

  private static Cli.Run run(String... args) {
    return Cli.run(Commands.all(), null, Golden.ROOT, args);
  }

  @Test
  void demoStartsHeadlessAndMovesTheMover() {
    Cli.Run run = run("--json", "launch", "run", "demo-unity", "--build");
    assertEquals(0, run.exitCode(), run.out() + run.err());
    assertTrue(run.out().contains("\"built\": true"), run.out());
    assertTrue(run.out().contains("\"gameExitCode\": 0"), run.out());
    assertTrue(run.out().startsWith("{") && run.out().endsWith("}\n"), "nur JSON auf stdout");
    assertTrue(run.err().contains("frames: 60"), run.err());
    assertTrue(run.err().contains("position=Vec3[x=5.0, y=0.0, z=0.0]"), run.err());
  }

  @Test
  void onlyStaleGameIsRebuilt() throws Exception {
    assertEquals(0, run("-q", "launch", "run", "demo-unity").exitCode());
    Cli.Run current = run("--json", "launch", "run", "demo-unity");
    assertTrue(current.out().contains("\"built\": false"), current.out());
    Files.writeString(DEMO.resolve(".port/build.hash"), "veraltet\n");
    Cli.Run stale = run("--json", "launch", "run", "demo-unity");
    assertTrue(stale.out().contains("\"built\": true"), stale.out());
  }

  @Test
  void textModePrintsGameOutputOnStdout() {
    Cli.Run run = run("launch", "run", "demo-unity");
    assertEquals(0, run.exitCode(), run.err());
    assertTrue(run.out().contains("frames: 60"), run.out());
    assertTrue(run.out().contains("port launch run: ok (Exit 0)"), run.out());
  }

  @Test
  void incompatibleOrMissingProfileGivesExit2() {
    assertEquals(2, run("launch", "run", "gibtsnicht").exitCode());
  }
}
