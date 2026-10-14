package io.github.localsoul.port.ir.refs;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.reader.IrReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Deklarationen und Referenzen an den IR-Fixtures aus M2. */
class DeclarationsAndReferencesTest {

  private static final Path GOLDEN = Path.of("..", "testdata", "golden", "ir", "unity");

  private static IrFile read(String name) throws Exception {
    Path path = GOLDEN.resolve(name);
    return new IrReader().read(Files.readString(path, StandardCharsets.UTF_8), "ir/" + name);
  }

  @Test
  void declarationsListTypeAndMembersWithKinds() throws Exception {
    List<String> found = new ArrayList<>();
    for (Declaration declaration : Declarations.collect(read("Inventory.ir"))) {
      found.add(declaration.kind() + " " + declaration.symbol() + " " + declaration.name());
    }
    assertEquals(
        List.of(
            "class Inventory Inventory",
            "field Inventory::counter counter",
            "field Inventory::items items",
            "field Inventory::label label",
            "method Inventory::Count# Count",
            "method Inventory::Fill#i32 Fill",
            "method Inventory::Name# Name",
            "method Inventory::SetCounter#System.Func<i32> SetCounter",
            "method Inventory::Total# Total"),
        found);
  }

  @Test
  void declarationsHavePositions() throws Exception {
    Declaration fill = null;
    for (Declaration declaration : Declarations.collect(read("Inventory.ir"))) {
      if (declaration.name().equals("Fill")) {
        fill = declaration;
      }
    }
    assertEquals("ir/Inventory.ir", fill.span().file());
    assertTrue(fill.span().line() > 3);
    assertEquals("Inventory", fill.type());
  }

  @Test
  void referencesNameSourceSymbolAndTarget() throws Exception {
    List<String> calls = new ArrayList<>();
    for (Reference reference : References.collect(read("Inventory.ir"))) {
      if (reference.member() != null && reference.symbol().equals("Inventory::Fill#i32")) {
        calls.add(reference.member().symbolId());
      }
    }
    assertTrue(calls.contains("Box::Put#T"), calls.toString());
    assertTrue(calls.contains("System.Collections.Generic.List::Add#T"), calls.toString());
    assertTrue(calls.contains("Inventory::items"), calls.toString());
  }

  @Test
  void enumMembersAreDeclarations() throws Exception {
    Path allNodes = Path.of("..", "testdata", "ir", "all-nodes.ir");
    IrFile file =
        new IrReader().read(Files.readString(allNodes, StandardCharsets.UTF_8), "all-nodes.ir");
    boolean enumMember = false;
    for (Declaration declaration : Declarations.collect(file)) {
      if (declaration.kind().equals("enum-member")) {
        enumMember = true;
        assertTrue(declaration.symbol().contains("::"), declaration.toString());
      }
    }
    assertTrue(enumMember);
  }
}
