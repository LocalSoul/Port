package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@code port lint} auf einem Projekt mit den IR-Fixtures (Plattform = dieses Repository). */
class LintCommandTest {

  @TempDir Path temp;

  private Path project;

  @BeforeEach
  void createProject() throws Exception {
    assertEquals(0, Cli.run(temp, "init", "demo").exitCode());
    project = temp.resolve("demo");
    try (DirectoryStream<Path> files = Files.newDirectoryStream(Fixtures.IR_GOLDEN, "*.ir")) {
      for (Path file : files) {
        Files.copy(file, project.resolve("ir").resolve(file.getFileName()));
      }
    }
    Cli.Run show = run("show", "UnsafeBits::First#i32[]");
    Matcher hash = Pattern.compile("\"irHash\": \"([0-9a-f]{12})").matcher(show.out());
    assertTrue(hash.find(), show.out());
    Path manual = project.resolve("manual/game/demo/UnsafeBits.java");
    Files.createDirectories(manual.getParent());
    Files.writeString(
        manual,
        "package game.demo;\n\npublic class UnsafeBits extends UnsafeBitsBase {\n"
            + "  // port:override UnsafeBits::First#i32[] ir="
            + hash.group(1)
            + "\n"
            + "  @Override\n  public int first(int[] values) {\n    return values[0];\n  }\n}\n");
  }

  private Cli.Run run(String... args) {
    String[] full = new String[args.length + 3];
    full[0] = "--json";
    full[1] = "-C";
    full[2] = "demo";
    System.arraycopy(args, 0, full, 3, args.length);
    return Cli.run(temp, full);
  }

  @Test
  void lintReportsFindingsAndExits9() throws Exception {
    Cli.Run run = run("lint", "--only");
    assertEquals(9, run.exitCode(), run.out());
    Golden.assertMatches("lint-fixtures.json", run.out());
    assertFalse(run.out().contains("UnsafeBits::First"), "Markierung deckt UnsafeBits ab");
  }

  @Test
  void filtersAndHints() {
    Cli.Run rule = run("lint", "--only", "--rule", "SH002");
    assertEquals(0, rule.exitCode(), rule.out());
    assertTrue(rule.out().contains("\"warnings\": 1"), rule.out());
    assertFalse(rule.out().contains("SH001"), rule.out());
    Cli.Run counter = run("lint", "--only", "--class", "Count*");
    assertTrue(counter.out().contains("Counter::Update#"), counter.out());
    assertFalse(counter.out().contains("Holder"), counter.out());
    assertTrue(run("lint", "--only").out().contains("\"hint\": null"));
    Cli.Run hints = run("lint", "--only", "--fix-hint", "--rule", "MN003");
    assertTrue(hints.out().contains("// port:override Edges::producer ir="), hints.out());
  }

  @Test
  void portTomlDisablesRulesAndChangesSeverity() throws Exception {
    Path manifest = project.resolve("port.toml");
    Files.writeString(
        manifest,
        Files.readString(manifest)
            + "\n[lint]\ndisable = [\"SH001\"]\n\n[lint.severity]\nSH002 = \"info\"\nMN003 = \"info\"\n");
    Cli.Run run = run("lint", "--only");
    assertEquals(0, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"errors\": 0,\n      \"warnings\": 0"), run.out());
  }

  @Test
  void chainReportsButDoesNotBlockUnlessStrict() {
    Cli.Run chain = run("index");
    assertEquals(0, chain.exitCode(), chain.out());
    assertTrue(chain.out().contains("\"build\": {"), chain.out());
    assertTrue(chain.out().contains("\"id\": \"SH001\""), chain.out());
    Cli.Run strict = run("--strict", "index");
    assertEquals(9, strict.exitCode(), strict.out());
    assertTrue(strict.out().contains("\"build\": {"), "baut trotzdem: " + strict.out());
  }
}
