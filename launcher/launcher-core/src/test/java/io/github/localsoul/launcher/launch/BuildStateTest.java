package io.github.localsoul.launcher.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class BuildStateTest {

  @TempDir Path game;

  private Path mapping;

  @BeforeEach
  void createGame() throws Exception {
    Files.createDirectories(game.resolve("ir"));
    Files.createDirectories(game.resolve("manual/a"));
    Files.writeString(game.resolve("port.toml"), "[project]\n");
    Files.writeString(game.resolve("ir/A.ir"), "(ir)\n");
    Files.writeString(game.resolve("manual/a/A.java"), "class A {}\n");
    mapping = game.resolve("mapping.tsv");
    Files.writeString(mapping, "x\ty\ttype\n");
  }

  @Test
  void hashChangesWithEveryInput() throws Exception {
    String base = BuildState.hash(game, List.of(mapping));
    assertEquals(base, BuildState.hash(game, List.of(mapping)));
    Files.writeString(game.resolve("ir/A.ir"), "(ir 2)\n");
    String ir = BuildState.hash(game, List.of(mapping));
    assertNotEquals(base, ir);
    Files.writeString(game.resolve("manual/a/A.java"), "class A { }\n");
    String manual = BuildState.hash(game, List.of(mapping));
    assertNotEquals(ir, manual);
    Files.writeString(mapping, "x\tz\ttype\n");
    assertNotEquals(manual, BuildState.hash(game, List.of(mapping)));
  }

  @Test
  void upToDateNeedsSameHashAndClasses() throws Exception {
    String hash = BuildState.hash(game, List.of(mapping));
    assertFalse(BuildState.upToDate(game, hash));
    BuildState.save(game, hash);
    assertFalse(BuildState.upToDate(game, hash), "ohne Klassen veraltet");
    Files.createDirectories(game.resolve(BuildState.CLASSES));
    assertTrue(BuildState.upToDate(game, hash));
    assertFalse(BuildState.upToDate(game, "anders"));
  }
}
