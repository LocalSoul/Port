package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
    Files.write(temp.resolve("downloads/UnityEngine.dll"), new byte[] {5});
    Files.writeString(temp.resolve("downloads/readme.txt"), "nicht kopieren");
    return dll;
  }

  @Test
  void createsProjectStructureOutsideTheMonorepo() throws Exception {
    assumeTrue(Fixtures.gitAvailable(), "git fehlt (Golden File enthält das Git-Ergebnis)");
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
    assertTrue(Files.isRegularFile(project.resolve("input/UnityEngine.dll")));
    assertFalse(Files.exists(project.resolve("input/readme.txt")));
  }

  @Test
  void createsOwnGitRepositoryWithFirstCommit() throws Exception {
    assumeTrue(Fixtures.gitAvailable(), "git fehlt");
    assembly();
    Cli.Run run =
        Cli.run(temp, "--json", "init", "demo", "--source", "downloads/Assembly-CSharp.dll");
    assertTrue(run.out().contains("\"git\": \"created\""), run.out());
    Path project = temp.resolve("demo");
    assertTrue(Files.isDirectory(project.resolve(".git")));
    String files = Fixtures.git(project, "ls-files");
    assertTrue(files.contains("port.toml"), files);
    assertFalse(files.contains("input/"), files);
    assertTrue(
        Fixtures.git(project, "log", "--format=%s")
            .contains("chore: game demo angelegt (port init)"));
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
    if (Fixtures.gitAvailable()) {
      assertTrue(again.out().contains("\"git\": \"exists\""), again.out());
    }
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

  private Path monorepo() throws Exception {
    Path root = temp.resolve("repo");
    Files.createDirectories(root.resolve("docs"));
    Files.writeString(
        root.resolve("pom.xml"),
        "<project><artifactId>recomp-parent</artifactId><modules/></project>");
    if (Fixtures.gitAvailable()) {
      Fixtures.git(root, "init", "-q", "-b", "main");
    }
    return root;
  }

  @Test
  void insideTheMonorepoProjectsGoToGamesAndBecomeSubmodules() throws Exception {
    assumeTrue(Fixtures.gitAvailable(), "git fehlt");
    Path root = monorepo();
    Cli.Run run = Cli.run(temp, "--json", "-C", "repo/docs", "init", "demo");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(Files.isRegularFile(root.resolve("games/demo/port.toml")));
    assertTrue(run.out().contains("\"path\": \"games/demo\""), run.out());
    assertTrue(run.out().contains("\"submodule\": true"), run.out());
    String modules = Files.readString(root.resolve(".gitmodules"));
    assertTrue(modules.contains("path = games/demo"), modules);
    assertTrue(modules.contains("url = ./games/demo"), modules);
    assertTrue(Fixtures.git(root, "diff", "--cached", "--name-only").contains("games/demo"));
    Cli.Run again = Cli.run(temp, "--json", "-C", "repo", "init", "demo");
    assertEquals(0, again.exitCode(), again.out());
    assertTrue(again.out().contains("\"git\": \"exists\""), again.out());
  }

  @Test
  void engineProjectsGoToEngines() throws Exception {
    assumeTrue(Fixtures.gitAvailable(), "git fehlt");
    Path root = monorepo();
    Cli.Run run = Cli.run(temp, "--json", "-C", "repo", "init", "godot", "--kind", "engine");
    assertEquals(0, run.exitCode(), run.out());
    Golden.assertMatches("init-engine.json", run.out());
    Golden.assertMatches(
        "init-engine-port.toml",
        Files.readString(root.resolve("engines/godot/port.toml"), StandardCharsets.UTF_8));
    assertTrue(Files.isDirectory(root.resolve("engines/godot/providers")));
    assertTrue(Files.readString(root.resolve(".gitmodules")).contains("path = engines/godot"));
    assertEquals(2, Cli.run(temp, "-C", "repo", "init", "x", "--kind", "plugin").exitCode());
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
