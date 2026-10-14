package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Suche und Navigation auf einem Projekt mit den IR-Fixtures (ohne .NET). */
class SearchCommandTest {

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
  void findAndOutlineMatchGoldenFiles() throws Exception {
    Cli.Run find = run("find", "inv");
    assertEquals(0, find.exitCode(), find.out());
    Golden.assertMatches("find-inv.json", find.out());
    Cli.Run outline = run("outline", "Inventory");
    assertEquals(0, outline.exitCode(), outline.out());
    Golden.assertMatches("outline-inventory.json", outline.out());
  }

  @Test
  void navigationCommands() {
    Cli.Run callers = run("callers", "Box::Put#T");
    assertTrue(callers.out().contains("\"symbol\": \"Inventory::Fill#i32\""), callers.out());
    Cli.Run callees = run("callees", "Inventory::Fill#i32");
    assertTrue(callees.out().contains("\"external\": true"), callees.out());
    Cli.Run uses = run("uses", "UnityEngine.Input");
    assertTrue(uses.out().contains("\"total\": 2"), uses.out());
    Cli.Run refs = run("refs", "Box");
    assertTrue(refs.out().contains("\"kind\": \"type\""), refs.out());
    Cli.Run show = run("show", "Box::Put#T");
    assertTrue(show.out().contains("\"ir\": \"(method Put void"), show.out());
    assertTrue(show.out().contains("\"javaFile\": null"), show.out());
    Cli.Run java = run("show", "Box::Put#T", "--java");
    assertTrue(java.out().contains("noch kein Java erzeugt"), java.out());
    Cli.Run src = run("show", "Box::Put#T", "--src");
    assertTrue(src.out().contains("PORT-LATER"), src.out());
    assertEquals(2, run("show", "Box::Put#T", "--ir", "--java").exitCode());
  }

  @Test
  void paginationAndUnknownSymbols() {
    Cli.Run first = run("find", "e", "--limit", "3");
    assertTrue(first.out().contains("\"next\": \"3\""), first.out());
    Cli.Run rest = run("find", "e", "--cursor", "3");
    assertTrue(rest.out().contains("\"next\": null"), rest.out());
    Cli.Run unknown = run("refs", "Gibt::Es#i32");
    assertEquals(2, unknown.exitCode());
    assertTrue(unknown.out().contains("\"hint\": \"port find Es\""), unknown.out());
    assertEquals(2, run("callers", "Fehlt::M#").exitCode());
    assertEquals(2, run("outline", "Inventory::Fill#i32").exitCode());
    assertEquals(2, run("find", "x", "--cursor", "abc").exitCode());
  }

  @Test
  void indexPhaseAndIrChanged() throws Exception {
    Cli.Run status = run("status");
    assertTrue(status.out().contains("\"irChanged\": 0"), status.out());
    Cli.Run index = run("index", "--only");
    assertEquals(0, index.exitCode(), index.out());
    assertTrue(index.out().contains("\"symbols\": "), index.out());
    Path box = project.resolve("ir/Box.ir");
    Files.writeString(
        box,
        Files.readString(box)
            .replace("(return (field this Box<T>::value))", "(return (default T))"));
    Cli.Run again = run("index", "--only");
    assertTrue(again.out().contains("\"changed\": [\n        \"ir/Box.ir\"\n      ]"), again.out());
    Cli.Run changed = run("status");
    assertTrue(changed.out().contains("\"irChanged\": 2"), changed.out());
    Cli.Run rebuilt = run("index", "--only", "--rebuild");
    assertEquals(0, rebuilt.exitCode(), rebuilt.out());
  }

  @Test
  void brokenIrGivesExit2WithLocation() throws Exception {
    Files.writeString(project.resolve("ir/Box.ir"), "(ir :version 1\n");
    Cli.Run run = run("find", "box");
    assertEquals(2, run.exitCode(), run.out());
    assertTrue(run.out().contains("\"location\": \"ir/Box.ir:"), run.out());
  }
}
