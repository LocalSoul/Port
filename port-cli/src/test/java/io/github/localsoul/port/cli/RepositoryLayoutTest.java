package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.manifest.GameManifest;
import io.github.localsoul.launcher.manifest.ManifestReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Monorepo-Layout (ADR-020, ADR-026): Projekte in {@code games/} und {@code engines/} als
 * Submodule. Ohne ausgecheckte Submodule ({@code git clone --recurse-submodules}) übersprungen.
 */
class RepositoryLayoutTest {

  private static final Path GAMES = Golden.ROOT.resolve("games");
  private static final Path DEMO = GAMES.resolve("demo");
  private static final Path UNITY = Golden.ROOT.resolve("engines/unity");

  @TempDir Path temp;

  private static void requireSubmodules() {
    assumeTrue(
        Files.isRegularFile(DEMO.resolve("port.toml"))
            && Files.isRegularFile(UNITY.resolve("port.toml")),
        "Submodule nicht ausgecheckt: git submodule update --init");
  }

  @Test
  void rootBuildContainsEngineModulesButNoGame() throws Exception {
    String pom = Files.readString(Golden.ROOT.resolve("pom.xml"), StandardCharsets.UTF_8);
    assertTrue(pom.contains("<module>engines/unity/adapter-unity</module>"), pom);
    assertFalse(pom.contains("<module>games/"), pom);
  }

  @Test
  void projectManifestsAreValid() throws Exception {
    requireSubmodules();
    EngineManifest unity =
        assertInstanceOf(
            EngineManifest.class, ManifestReader.read(UNITY, "engines/unity/port.toml"));
    assertTrue(Files.isDirectory(UNITY.resolve(unity.module())));
    try (DirectoryStream<Path> games = Files.newDirectoryStream(GAMES)) {
      for (Path game : games) {
        GameManifest manifest =
            assertInstanceOf(
                GameManifest.class, ManifestReader.read(game, game.getFileName() + "/port.toml"));
        assertEquals(unity.adapter(), manifest.requiresAdapter(), game.toString());
      }
    }
  }

  @Test
  void repositoryProfilesAreCompatible() {
    requireSubmodules();
    for (String profile : new String[] {"demo-unity", "starcollector-unity"}) {
      Cli.Run check =
          Cli.run(Commands.all(), null, Golden.ROOT, "--json", "launch", "check", profile);
      assertEquals(0, check.exitCode(), check.out());
    }
  }

  @Test
  void demoIrMatchesTheFrontendGoldenFiles() throws Exception {
    requireSubmodules();
    try (DirectoryStream<Path> golden = Files.newDirectoryStream(Fixtures.IR_GOLDEN, "*.ir")) {
      for (Path file : golden) {
        Path actual = DEMO.resolve("ir").resolve(file.getFileName());
        assertTrue(Files.isRegularFile(actual), actual.toString());
        assertEquals(
            Files.readString(file, StandardCharsets.UTF_8),
            Files.readString(actual, StandardCharsets.UTF_8)
                .replaceFirst("\\s+:sha256 \"[0-9a-f]+\"", ""),
            file.getFileName().toString());
      }
    }
  }

  @Test
  void demoStillBuildsWithPortBuild() throws Exception {
    requireSubmodules();
    Path copy = temp.resolve("demo");
    try (Stream<Path> walk = Files.walk(DEMO)) {
      for (Path source : (Iterable<Path>) walk::iterator) {
        Path relative = DEMO.relativize(source);
        String first = relative.getNameCount() > 0 ? relative.getName(0).toString() : "";
        if (first.equals(".git")
            || first.equals("gen")
            || first.equals(".port")
            || first.equals("input")) {
          continue;
        }
        Path target = copy.resolve(relative.toString());
        if (Files.isDirectory(source)) {
          Files.createDirectories(target);
        } else {
          Files.copy(source, target);
        }
      }
    }
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "gen");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(Files.isRegularFile(copy.resolve(".port/classes/game/demo/UnsafeBits.class")));
  }
}
