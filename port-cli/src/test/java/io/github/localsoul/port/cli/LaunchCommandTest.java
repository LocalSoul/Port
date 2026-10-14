package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.launcher.catalog.MiniRepo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code port launch new/list/check} gegen ein synthetisches Mini-Repo. */
class LaunchCommandTest {

  @TempDir Path temp;

  private Path repo;

  @BeforeEach
  void createRepo() throws Exception {
    repo = temp.resolve("repo");
    MiniRepo.create(repo);
  }

  private Cli.Run run(String... args) {
    return Cli.run(Commands.all(), null, repo, args);
  }

  @Test
  void newWritesProfileAndListShowsIt() throws Exception {
    Cli.Run created =
        run("--json", "launch", "new", "demo-unity", "--game", "demo", "--engine", "unity");
    assertEquals(0, created.exitCode(), created.out());
    Golden.assertMatches(
        "launch-new-profile.toml",
        Files.readString(repo.resolve("profiles/demo-unity.toml"), StandardCharsets.UTF_8));
    Cli.Run again =
        run("--json", "launch", "new", "demo-unity", "--game", "demo", "--engine", "unity");
    assertTrue(again.out().contains("\"created\": false"), again.out());
    assertEquals(
        2, run("launch", "new", "demo-unity", "--game", "demo", "--engine", "godot").exitCode());
    Cli.Run list = run("--json", "launch", "list");
    assertTrue(list.out().contains("\"name\": \"demo-unity\""), list.out());
    Cli.Run set =
        run(
            "--json",
            "launch",
            "new",
            "b",
            "--game",
            "demo",
            "--engine",
            "unity",
            "--set",
            "input=x");
    assertTrue(set.out().contains("PORT-LATER"), set.out());
  }

  @Test
  void unknownGameOrEngineSuggestsCatalog() {
    Cli.Run game = run("--json", "launch", "new", "p", "--game", "fehlt", "--engine", "unity");
    assertEquals(2, game.exitCode());
    assertTrue(game.out().contains("\"hint\": \"port catalog list --kind game\""), game.out());
    assertEquals(2, run("launch", "new", "p", "--game", "demo", "--engine", "fehlt").exitCode());
  }

  @Test
  void checkPassesForMatchingProfile() {
    run("launch", "new", "demo-unity", "--game", "demo", "--engine", "unity");
    Cli.Run check = run("--json", "launch", "check", "demo-unity");
    assertEquals(0, check.exitCode(), check.out());
    assertTrue(check.out().contains("\"compatible\": true"), check.out());
  }

  @Test
  void checkFailureNamesNextCommand() throws Exception {
    run("launch", "new", "demo-godot", "--game", "demo", "--engine", "godot");
    Cli.Run check = run("--json", "launch", "check", "demo-godot");
    assertEquals(2, check.exitCode(), check.out());
    Golden.assertMatches("launch-check-wrong-adapter.json", check.out());
    Cli.Run missing = run("--json", "launch", "check", "fehlt");
    assertEquals(2, missing.exitCode());
    assertTrue(missing.out().contains("port launch list"), missing.out());
  }
}
