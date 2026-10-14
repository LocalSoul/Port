package io.github.localsoul.port.index;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Index über die IR-Fixtures aus M2 (kopiert nach {@code @TempDir/ir}). */
class IrIndexTest {

  private static final Path GOLDEN = Path.of("..", "testdata", "golden", "ir", "unity");

  @TempDir Path project;

  @BeforeEach
  void copyIr() throws Exception {
    Files.createDirectories(project.resolve("ir"));
    try (DirectoryStream<Path> files = Files.newDirectoryStream(GOLDEN, "*.ir")) {
      for (Path file : files) {
        Files.copy(file, project.resolve("ir").resolve(file.getFileName()));
      }
    }
  }

  private static List<String> sources(List<RefRow> rows) {
    List<String> result = new ArrayList<>();
    for (RefRow row : rows) {
      result.add(row.source());
    }
    return result;
  }

  private static List<String> ids(List<SymbolRow> rows) {
    List<String> result = new ArrayList<>();
    for (SymbolRow row : rows) {
      result.add(row.symbol());
    }
    return result;
  }

  @Test
  void firstRunReadsEverythingSecondRunNothing() throws Exception {
    try (IrIndex index = IrIndex.open(project, false)) {
      UpdateResult first = index.update();
      assertEquals(9, first.added().size());
      assertTrue(first.symbols() > 40, "Symbole: " + first.symbols());
      assertEquals(first.symbols(), first.changes().size());
      UpdateResult second = index.update();
      assertTrue(second.unchanged());
      assertEquals(List.of(), second.changes());
      assertEquals(
          first.changes(), index.changes(), "Lauf ohne Änderung behält die letzten Änderungen");
    }
  }

  @Test
  void changedFileIsReadAloneAndChangesPerSymbolAreRecorded() throws Exception {
    try (IrIndex index = IrIndex.open(project, false)) {
      index.update();
      Path box = project.resolve("ir/Box.ir");
      Files.writeString(
          box,
          Files.readString(box)
              .replace("(return (field this Box<T>::value))", "(return (default T))"));
      Files.delete(project.resolve("ir/Edges.ir"));
      UpdateResult result = index.update();
      assertEquals(List.of("ir/Box.ir"), result.changed());
      assertEquals(List.of("ir/Edges.ir"), result.removed());
      assertEquals(List.of(), result.added());
      List<String> changes = new ArrayList<>();
      for (Change change : result.changes()) {
        changes.add(change.change() + " " + change.symbol());
      }
      assertEquals(
          List.of(
              "changed Box",
              "changed Box::Get#",
              "removed Edges",
              "removed Edges::Angle#i32,i32",
              "removed Edges::Produce#",
              "removed Edges::producer"),
          changes);
      assertEquals(result.changes(), index.changes());
      assertNull(index.symbol("Edges"));
    }
  }

  @Test
  void callersCalleesUsesRefsOutline() throws Exception {
    try (IrIndex index = IrIndex.open(project, false)) {
      index.update();
      assertEquals(List.of("Inventory::Fill#i32"), sources(index.callers("Box::Put#T")));
      List<String> callees = new ArrayList<>();
      for (RefRow row : index.callees("Inventory::Fill#i32")) {
        callees.add(row.target() + (row.external() ? " extern" : ""));
      }
      assertEquals(
          List.of(
              "Box::Put#T",
              "System.Collections.Generic.List::Add#T extern",
              "System.Collections.Generic.List::Clear# extern",
              "System.Collections.Generic.List::RemoveAt#i32 extern",
              "System.Collections.Generic.List::set_Item#i32,T extern"),
          callees);
      List<String> uses = new ArrayList<>();
      for (RefRow row : index.uses("UnityEngine.Input")) {
        uses.add(row.source() + " -> " + row.target());
      }
      assertEquals(
          List.of(
              "Mover::Update# -> UnityEngine.Input::GetAxis#string",
              "Counter::Update# -> UnityEngine.Input::GetKey#string"),
          uses);
      assertTrue(sources(index.refs("Box")).contains("Inventory::label"));
      assertEquals(
          List.of(
              "Inventory::counter",
              "Inventory::items",
              "Inventory::label",
              "Inventory::Count#",
              "Inventory::Fill#i32",
              "Inventory::Name#",
              "Inventory::SetCounter#System.Func<i32>",
              "Inventory::Total#"),
          ids(index.outline("Inventory")));
      assertEquals(List.of(), index.uses("Inventory"), "eigene Typen sind keine externe API");
    }
  }

  @Test
  void findIgnoresCaseAndHandlesShortText() throws Exception {
    try (IrIndex index = IrIndex.open(project, false)) {
      index.update();
      List<String> mover = ids(index.find("mOvEr"));
      assertTrue(mover.contains("Mover"), mover.toString());
      assertTrue(mover.contains("Mover::Update#"), mover.toString());
      assertEquals(ids(index.find("mover")), mover);
      assertTrue(ids(index.find("fi")).contains("Inventory::Fill#i32"));
      assertEquals(List.of(), index.find("gibtsnicht"));
      assertEquals(List.of(), index.find("a\"b"));
    }
  }

  @Test
  void irTextIsTheDeclaration() throws Exception {
    try (IrIndex index = IrIndex.open(project, false)) {
      index.update();
      SymbolRow fill = index.symbol("Inventory::Fill#i32");
      String text = index.irText(fill);
      assertTrue(text.startsWith("(method Fill void ((count i32))"), text);
      assertTrue(text.endsWith(")"), text);
      assertTrue(fill.location().startsWith("ir/Inventory.ir:"), fill.location());
    }
  }

  @Test
  void invalidIrRollsBackAndKeepsThePreviousState() throws Exception {
    try (IrIndex index = IrIndex.open(project, false)) {
      int symbols = index.update().symbols();
      Files.writeString(project.resolve("ir/Box.ir"), "(ir :version 1\n");
      Files.delete(project.resolve("ir/Edges.ir"));
      IndexException e = assertThrows(IndexException.class, index::update);
      assertEquals(IndexException.INVALID, e.exitCode());
      assertTrue(e.location().startsWith("ir/Box.ir:"), e.location());
      assertTrue(index.symbol("Edges") != null, "Rollback: Edges noch im Index");
      assertEquals(symbols, index.find("").size());
    }
  }

  @Test
  void rebuildGivesTheSameResult() throws Exception {
    List<String> first;
    try (IrIndex index = IrIndex.open(project, false)) {
      index.update();
      first = ids(index.find(""));
    }
    try (IrIndex index = IrIndex.open(project, true)) {
      assertFalse(index.update().unchanged());
      assertEquals(first, ids(index.find("")));
    }
  }
}
