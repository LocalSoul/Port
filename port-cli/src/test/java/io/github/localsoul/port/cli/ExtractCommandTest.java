package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code port extract}: Existenz und SHA-256 der Eingabe, Zustand, Fehlerfälle. */
class ExtractCommandTest {

  @TempDir Path temp;

  @BeforeEach
  void createProject() throws Exception {
    Files.write(temp.resolve("game.dll"), new byte[] {7, 7, 7});
    assertEquals(0, Cli.run(temp, "init", "demo", "--source", "game.dll").exitCode());
  }

  @Test
  void checksInputAndRecordsIt() throws Exception {
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "extract", "--only");
    assertEquals(0, run.exitCode(), run.out());
    Golden.assertMatches("extract-only.json", run.out());
    assertEquals("phase\textract\n", Files.readString(temp.resolve("demo/.port/state.tsv")));
    assertTrue(Files.isRegularFile(temp.resolve("demo/.port/extract.tsv")));
  }

  @Test
  void hashMismatchGivesExit3() throws Exception {
    Files.write(temp.resolve("demo/input/game.dll"), new byte[] {8});
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "extract", "--only");
    assertEquals(3, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"id\": \"PORT-HASH\""), run.out());
    assertTrue(run.out().contains("\"phases\": [\n      \"extract\"\n    ]"), run.out());
  }

  @Test
  void missingInputGivesExit3() throws Exception {
    Files.delete(temp.resolve("demo/input/game.dll"));
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "extract", "--only");
    assertEquals(3, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"location\": \"input/game.dll\""), run.out());
  }

  @Test
  void missingHashIsOnlyAHint() throws Exception {
    Path manifest = temp.resolve("demo/port.toml");
    Files.writeString(manifest, Files.readString(manifest).replaceAll("sha256 = .*\n", ""));
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "extract", "--only");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"severity\": \"info\""), run.out());
  }

  @Test
  void invalidManifestOrNoProjectGivesExit2() throws Exception {
    Cli.Run outside = Cli.run(temp, "--json", "extract");
    assertEquals(2, outside.exitCode(), outside.out());
    assertTrue(outside.out().contains("PORT-MANIFEST"), outside.out());
    Files.writeString(temp.resolve("demo/port.toml"), "[project\n");
    Cli.Run broken = Cli.run(temp, "--json", "-C", "demo", "extract");
    assertEquals(2, broken.exitCode(), broken.out());
    assertTrue(broken.out().contains("\"location\": \"port.toml:1:"), broken.out());
  }

  @Test
  void unknownStartPhaseGivesExit2() {
    assertEquals(2, Cli.run(temp, "-C", "demo", "extract", "--from", "lint").exitCode());
  }
}
