package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code port ir} mit dem echten Frontend auf dem synthetischen Testspiel. */
class IrCommandTest {

  @TempDir Path temp;

  private Path project() {
    Cli.Run init = Cli.run(temp, "init", "demo", "--source", Fixtures.ASSEMBLY.toString());
    assertEquals(0, init.exitCode(), init.err());
    return temp.resolve("demo");
  }

  private static List<String> irFiles(Path dir) throws Exception {
    List<String> names = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.ir")) {
      for (Path path : stream) {
        names.add(path.getFileName().toString());
      }
    }
    names.sort(null);
    return names;
  }

  @Test
  void writesCanonicalIrPerTypeLikeTheGoldenFiles() throws Exception {
    assumeTrue(Fixtures.frontendAvailable(), Fixtures.SKIP);
    Path project = project();
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "ir", "--only");
    assertEquals(0, run.exitCode(), run.out() + run.err());
    assertEquals(irFiles(Fixtures.IR_GOLDEN), irFiles(project.resolve("ir")));
    for (String name : irFiles(Fixtures.IR_GOLDEN)) {
      String actual = Files.readString(project.resolve("ir").resolve(name), StandardCharsets.UTF_8);
      assertTrue(actual.contains(":sha256 \""), name);
      String golden = Files.readString(Fixtures.IR_GOLDEN.resolve(name), StandardCharsets.UTF_8);
      assertEquals(golden, actual.replaceFirst("\\s+:sha256 \"[0-9a-f]+\"", ""), name);
    }
  }

  @Test
  void secondRunWritesNothingAndStaleFilesAreRemoved() throws Exception {
    assumeTrue(Fixtures.frontendAvailable(), Fixtures.SKIP);
    Path project = project();
    Cli.run(temp, "-C", "demo", "ir", "--only");
    Path mover = project.resolve("ir/Mover.ir");
    Files.setLastModifiedTime(mover, FileTime.fromMillis(1_000_000L));
    Files.writeString(project.resolve("ir/Old.ir"), "(ir :version 1)\n");
    Cli.Run again = Cli.run(temp, "--json", "-C", "demo", "ir", "--only");
    assertEquals(0, again.exitCode(), again.out());
    assertTrue(again.out().contains("\"written\": []"), again.out());
    assertTrue(again.out().contains("\"ir/Old.ir\""), again.out());
    assertFalse(Files.exists(project.resolve("ir/Old.ir")));
    assertEquals(FileTime.fromMillis(1_000_000L), Files.getLastModifiedTime(mover));
  }

  @Test
  void missingReferenceAssembliesAreReported() throws Exception {
    assumeTrue(Fixtures.frontendAvailable(), Fixtures.SKIP);
    Path alone = temp.resolve("allein/Assembly-CSharp.dll");
    Files.createDirectories(alone.getParent());
    Files.copy(Fixtures.ASSEMBLY, alone);
    Cli.run(temp, "init", "demo", "--source", alone.toString());
    Cli.Run run = Cli.run(temp, "--json", "-C", "demo", "ir", "--only");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"id\": \"PORT-UNRESOLVED\""), run.out());
    assertTrue(run.out().contains("\"location\": \"ir/Mover.ir\""), run.out());
  }

  @Test
  void missingFrontendGivesClearMessage() throws Exception {
    assumeFalse(Fixtures.onPath("port-frontend-unity"), "port-frontend-unity liegt im PATH");
    assumeTrue(System.getenv("PORT_FRONTEND_UNITY") == null, "PORT_FRONTEND_UNITY ist gesetzt");
    Files.write(temp.resolve("game.dll"), new byte[] {1});
    Cli.run(temp, "init", "demo", "--source", "game.dll");
    Path noPlatform = temp.resolve("leer");
    Cli.Run run = Cli.run(Commands.all(), noPlatform, temp, "--json", "-C", "demo", "ir", "--only");
    assertEquals(1, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"id\": \"PORT-FRONTEND\""), run.out());
    assertTrue(run.out().contains("scripts/build-dotnet.sh"), run.out());
  }
}
