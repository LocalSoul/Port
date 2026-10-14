package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.launcher.catalog.MiniRepo;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code port catalog list/show} gegen ein synthetisches Mini-Repo. */
class CatalogCommandTest {

  @TempDir Path temp;

  private Path repo;

  @BeforeEach
  void createRepo() throws Exception {
    repo = temp.resolve("repo");
    MiniRepo.create(repo);
  }

  private Cli.Run run(String... args) {
    return Cli.run(java.util.List.copyOf(Commands.all()), null, repo, args);
  }

  @Test
  void listIsSortedAndReportsBrokenProjects() throws Exception {
    Cli.Run run = run("--json", "catalog", "list");
    assertEquals(0, run.exitCode(), run.out());
    Golden.assertMatches("catalog-list.json", run.out());
  }

  @Test
  void paginationWithLimitAndCursor() {
    Cli.Run first = run("--json", "catalog", "list", "--limit", "2");
    assertTrue(first.out().contains("\"next\": \"2\""), first.out());
    assertTrue(first.out().contains("\"total\": 4"), first.out());
    Cli.Run second = run("--json", "catalog", "list", "--limit", "2", "--cursor", "2");
    assertTrue(second.out().contains("\"next\": null"), second.out());
    assertTrue(second.out().contains("input-godot-like"), second.out());
    assertEquals(2, run("catalog", "list", "--cursor", "abc").exitCode());
    assertEquals(2, run("catalog", "list", "--cursor", "9").exitCode());
    Cli.Run engines = run("--json", "catalog", "list", "--kind", "engine");
    assertTrue(engines.out().contains("\"total\": 2"), engines.out());
    assertEquals(2, run("catalog", "list", "--kind", "plugin").exitCode());
  }

  @Test
  void showEngineWithCoverage() throws Exception {
    Cli.Run run = run("--json", "catalog", "show", "unity");
    assertEquals(0, run.exitCode(), run.out());
    Golden.assertMatches("catalog-show-engine.json", run.out());
    Cli.Run game = run("--json", "catalog", "show", "demo");
    assertTrue(game.out().contains("\"api\": \">=2022.3\""), game.out());
    Cli.Run missing = run("--json", "catalog", "show", "gibtsnicht");
    assertEquals(2, missing.exitCode());
    assertTrue(missing.out().contains("\"hint\": \"port catalog list\""), missing.out());
  }

  @Test
  void groupWithoutSubcommandAndHelp() {
    assertEquals(2, run("catalog").exitCode());
    assertEquals(0, run("catalog", "--help").exitCode());
    assertEquals(0, run("catalog", "list", "--help").exitCode());
  }

  @Test
  void outsideMonorepoWithoutHomeGivesExit2() {
    Cli.Run run = Cli.run(Commands.all(), null, temp, "--json", "catalog", "list");
    assertEquals(2, run.exitCode(), run.out());
  }
}
