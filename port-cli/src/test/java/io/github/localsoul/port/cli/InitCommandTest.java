package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.core.ProjectFiles;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code port init} außerhalb und innerhalb des Monorepos. */
class InitCommandTest {

  @TempDir Path temp;

  private Path assembly() throws Exception {
    Path dll = temp.resolve("downloads/Assembly-CSharp.dll");
    Files.createDirectories(dll.getParent());
    Files.write(dll, new byte[] {1, 2, 3, 4});
    return dll;
  }

  @Test
  void createsProjectStructureOutsideTheMonorepo() throws Exception {
    assembly();
    Cli.Run run =
        Cli.run(temp, "--json", "init", "demo", "--source", "downloads/Assembly-CSharp.dll");
    assertEquals(0, run.exitCode(), run.out());
    Golden.assertMatches("init-outside-monorepo.json", run.out());
    Path project = temp.resolve("demo");
    for (String dir : new String[] {"input", "ir", "manual", "data"}) {
      assertTrue(Files.isDirectory(project.resolve(dir)), dir);
    }
    assertEquals(
        ProjectFiles.sha256(temp.resolve("downloads/Assembly-CSharp.dll")),
        ProjectFiles.sha256(project.resolve("input/Assembly-CSharp.dll")));
    Golden.assertMatches(
        "init-port.toml", Files.readString(project.resolve("port.toml"), StandardCharsets.UTF_8));
    String gitignore = Files.readString(project.resolve(".gitignore"));
    assertTrue(gitignore.contains("input/\ngen/\n.port/\n"), gitignore);
    assertFalse(Files.exists(project.resolve(".git")));
  }

  @Test
  void secondRunChangesNothing() throws Exception {
    assembly();
    Cli.run(temp, "init", "demo", "--source", "downloads/Assembly-CSharp.dll");
    Path manifest = temp.resolve("demo/port.toml");
    FileTime before = Files.getLastModifiedTime(manifest);
    Files.setLastModifiedTime(manifest, FileTime.fromMillis(1_000_000L));
    Cli.Run again =
        Cli.run(temp, "--json", "init", "demo", "--source", "downloads/Assembly-CSharp.dll");
    assertEquals(0, again.exitCode(), again.out());
    assertTrue(again.out().contains("\"created\": []"), again.out());
    assertTrue(again.out().contains("PORT-EXISTS"), again.out());
    assertEquals(FileTime.fromMillis(1_000_000L), Files.getLastModifiedTime(manifest));
    assertTrue(before.toMillis() > 0);
  }

  @Test
  void differentManifestIsRejectedAndLeftAlone() throws Exception {
    assembly();
    Cli.run(temp, "init", "demo", "--source", "downloads/Assembly-CSharp.dll");
    String manifest = Files.readString(temp.resolve("demo/port.toml"));
    Cli.Run run = Cli.run(temp, "--json", "init", "demo");
    assertEquals(2, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"id\": \"PORT-EXISTS\""), run.out());
    assertEquals(manifest, Files.readString(temp.resolve("demo/port.toml")));
  }

  @Test
  void insideTheMonorepoProjectsGoToGames() throws Exception {
    Path root = temp.resolve("repo");
    Files.createDirectories(root.resolve("docs"));
    Files.writeString(
        root.resolve("pom.xml"),
        "<project><artifactId>recomp-parent</artifactId><modules/></project>");
    Cli.Run run = Cli.run(temp, "--json", "-C", "repo/docs", "init", "demo");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(Files.isRegularFile(root.resolve("games/demo/port.toml")));
    assertTrue(run.out().contains("\"path\": \"games/demo\""), run.out());
    assertFalse(run.out().contains("PORT-GIT"), run.out());
  }

  @Test
  void invalidArgumentsAndMissingSource() {
    assertEquals(2, Cli.run(temp, "init", "Demo!").exitCode());
    assertEquals(2, Cli.run(temp, "init", "demo", "--frontend", "godot").exitCode());
    assertEquals(2, Cli.run(temp, "init").exitCode());
    Cli.Run missing = Cli.run(temp, "--json", "init", "demo", "--source", "fehlt.dll");
    assertEquals(3, missing.exitCode());
    assertFalse(Files.exists(temp.resolve("demo")));
  }
}
