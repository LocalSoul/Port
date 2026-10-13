package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * M4-Abnahme: {@code init → extract → ir → gen → build → status} auf dem synthetischen Testspiel
 * ({@code testdata/unity-sources}), Plattform = dieses Repository.
 */
class PipelineEndToEndTest {

  private static final Path MANUAL_UNSAFE_BITS =
      Golden.ROOT.resolve(
          "integration-tests/src/test/resources/manual/gen/fixtures/UnsafeBits.java");

  @TempDir Path temp;

  @BeforeEach
  void requireFrontend() {
    assumeTrue(Fixtures.frontendAvailable(), Fixtures.SKIP);
  }

  private Path init(String name) {
    Cli.Run run = Cli.run(temp, "init", name, "--source", Fixtures.ASSEMBLY.toString());
    assertEquals(0, run.exitCode(), run.err());
    return temp.resolve(name);
  }

  private static void addManualUnsafeBits(Path project) throws Exception {
    String source = Files.readString(MANUAL_UNSAFE_BITS, StandardCharsets.UTF_8);
    Path target = project.resolve("manual/game/demo/UnsafeBits.java");
    Files.createDirectories(target.getParent());
    Files.writeString(target, source.replace("package gen.fixtures;", "package game.demo;"));
  }

  /** Inhalt aller Dateien unter {@code ir/}, {@code gen/} und {@code .port/classes}. */
  private static Map<String, String> outputs(Path project) throws Exception {
    Map<String, String> result = new TreeMap<>();
    for (String dir : new String[] {"ir", "gen", ".port/classes"}) {
      try (Stream<Path> walk = Files.walk(project.resolve(dir))) {
        for (Path file : (Iterable<Path>) walk::iterator) {
          if (Files.isRegularFile(file)) {
            result.put(
                project.relativize(file).toString(),
                java.util.HexFormat.of().formatHex(Files.readAllBytes(file)));
          }
        }
      }
    }
    return result;
  }

  @Test
  void fullPipelineRunsAndStatusIsConsistent() throws Exception {
    Path project = init("demo");
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "extract");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(
        run.out()
            .contains(
                "\"phases\": [\n      \"extract\",\n      \"ir\",\n      \"gen\",\n      \"build\"\n    ]"),
        run.out());
    assertTrue(Files.isRegularFile(project.resolve("gen/game/demo/MoverBase.java")));
    assertTrue(Files.isRegularFile(project.resolve(".port/classes/game/demo/Mover.class")));
    Cli.Run status = Cli.run(temp, "--json", "-C", "demo", "status");
    assertEquals(0, status.exitCode(), status.out());
    // UnsafeBits (fixed-Block) und Edges::producer (Lambda) sind die raw-Stellen der Fixtures.
    Golden.assertMatches("status-after-build.json", status.out());
    addManualUnsafeBits(project);
    Cli.Run withManual = Cli.run(temp, "--json", "-C", "demo", "gen");
    assertEquals(0, withManual.exitCode(), withManual.out());
    assertTrue(Files.isRegularFile(project.resolve(".port/classes/game/demo/UnsafeBits.class")));
    Cli.Run after = Cli.run(temp, "--json", "-C", "demo", "status");
    assertTrue(after.out().contains("\"manualOpen\": 1"), after.out());
  }

  @Test
  void secondRunChangesNoFile() throws Exception {
    Path project = init("demo");
    addManualUnsafeBits(project);
    assertEquals(0, Cli.run(temp, "-C", "demo", "extract").exitCode());
    Path generated = project.resolve("gen/game/demo/MoverBase.java");
    Path compiled = project.resolve(".port/classes/game/demo/MoverBase.class");
    Files.setLastModifiedTime(generated, FileTime.fromMillis(1_000_000L));
    Files.setLastModifiedTime(compiled, FileTime.fromMillis(1_000_000L));
    Map<String, String> first = outputs(project);
    Cli.Run again = Cli.run(temp, "--json", "-C", "demo", "extract");
    assertEquals(0, again.exitCode(), again.out());
    assertEquals(first, outputs(project));
    assertEquals(FileTime.fromMillis(1_000_000L), Files.getLastModifiedTime(generated));
    assertEquals(FileTime.fromMillis(1_000_000L), Files.getLastModifiedTime(compiled));
    assertTrue(!again.out().contains("\"written\": [\n"), again.out());
  }

  @Test
  void twoProjectsProduceByteIdenticalOutput() throws Exception {
    Path a = init("demo");
    assertEquals(0, Cli.run(temp, "-C", "demo", "extract").exitCode());
    Path other = temp.resolve("zweiter");
    Files.createDirectories(other);
    assertEquals(
        0, Cli.run(other, "init", "demo", "--source", Fixtures.ASSEMBLY.toString()).exitCode());
    assertEquals(0, Cli.run(other, "-C", "demo", "extract").exitCode());
    assertEquals(outputs(a), outputs(other.resolve("demo")));
  }

  @Test
  void strictTurnsOpenManualWorkIntoExit5() {
    init("demo");
    Cli.Run run = Cli.run(temp, "--json", "--strict", "-C", "demo", "extract");
    assertEquals(5, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"id\": \"PORT-MANUAL\""), run.out());
    assertEquals(5, Cli.run(temp, "--strict", "-C", "demo", "status").exitCode());
  }

  @Test
  void compileErrorGivesExit7WithLocation() throws Exception {
    Path project = init("demo");
    assertEquals(0, Cli.run(temp, "-C", "demo", "extract", "--from", "ir").exitCode());
    Path broken = project.resolve("manual/game/demo/UnsafeBits.java");
    Files.createDirectories(broken.getParent());
    Files.writeString(
        broken,
        "package game.demo;\n\npublic class UnsafeBits extends UnsafeBitsBase {\n  int x = \"text\";\n}\n");
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "build", "--only");
    assertEquals(7, run.exitCode(), run.out());
    assertTrue(
        run.out().contains("\"location\": \"manual/game/demo/UnsafeBits.java:4:"), run.out());
  }

  @Test
  void unknownAdapterGivesExit8() {
    init("demo");
    assertEquals(0, Cli.run(temp, "-C", "demo", "ir", "--only").exitCode());
    Cli.Run run =
        Cli.run(temp, "--json", "-C", "demo", "gen", "--only", "--adapter", "engines/gibtsnicht");
    assertEquals(8, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"id\": \"PORT-PLATFORM\""), run.out());
  }

  @Test
  void unreadableAssemblyGivesExit3() throws Exception {
    Files.writeString(temp.resolve("kaputt.dll"), "keine Assembly");
    assertEquals(0, Cli.run(temp, "init", "demo", "--source", "kaputt.dll").exitCode());
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "ir");
    assertEquals(3, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"id\": \"PORT-FRONTEND\""), run.out());
  }

  @Test
  void frontendWithoutOutputGivesTranslatorExit4() throws Exception {
    Path fake = temp.resolve("fake-frontend.sh");
    Files.writeString(fake, "#!/bin/sh\nexit 0\n");
    assumeTrue(fake.toFile().setExecutable(true), "Skript nicht ausführbar");
    init("demo");
    String previous = System.getProperty("port.frontend.unity");
    System.setProperty("port.frontend.unity", fake.toString());
    try {
      Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "ir");
      assertEquals(4, run.exitCode(), run.out());
      assertTrue(run.out().contains("\"id\": \"PORT-FRONTEND\""), run.out());
    } finally {
      if (previous == null) {
        System.clearProperty("port.frontend.unity");
      } else {
        System.setProperty("port.frontend.unity", previous);
      }
    }
  }

  @Test
  void genWithClassFilterWritesOnlyMatchingTypes() {
    init("demo");
    assertEquals(0, Cli.run(temp, "-C", "demo", "ir", "--only").exitCode());
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "gen", "--only", "--class", "Mov*");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(
        run.out()
            .contains(
                "\"files\": [\n        \"gen/game/demo/Mover.java\",\n        \"gen/game/demo/MoverBase.java\"\n      ]"),
        run.out());
    assertTrue(!Files.exists(temp.resolve("demo/gen/game/demo/BoxBase.java")));
  }
}
