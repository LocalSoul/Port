package io.github.localsoul.port.ir.reader;

import static io.github.localsoul.port.ir.reader.Snippets.HEADER;
import static io.github.localsoul.port.ir.reader.Snippets.read;
import static io.github.localsoul.port.ir.reader.Snippets.rejects;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.BlockStmt;
import io.github.localsoul.port.ir.ast.CallExpr;
import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.ExprStmt;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.FieldExpr;
import io.github.localsoul.port.ir.ast.IdentExpr;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.LocalStmt;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Modifier;
import io.github.localsoul.port.ir.ast.NewExpr;
import io.github.localsoul.port.ir.ast.NumberLit;
import io.github.localsoul.port.ir.ast.PropExpr;
import io.github.localsoul.port.ir.ast.StaticCallExpr;
import io.github.localsoul.port.ir.ast.StaticPropExpr;
import io.github.localsoul.port.ir.ast.StringLit;
import io.github.localsoul.port.ir.ast.ThisExpr;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class IrReaderTest {

  private static final Path SPEC_EXAMPLE = Path.of("..", "testdata", "ir", "spec-example.ir");

  private static String pos(int line, int column) {
    return "spec-example.ir:" + line + ":" + column;
  }

  @Test
  void readsSpecExampleWithExpectedPositions() throws IOException, IrSyntaxException {
    String text = Files.readString(SPEC_EXAMPLE, StandardCharsets.UTF_8);
    IrFile file = new IrReader().read(text, "spec-example.ir");

    assertEquals(1, file.header().version());
    assertEquals("Assembly-CSharp", file.header().assembly());
    assertEquals("ab12...", file.header().sha256());
    assertEquals(pos(1, 1), file.header().span().toString());

    ClassDecl mover = assertInstanceOf(ClassDecl.class, file.types().get(0));
    assertEquals("Mover", mover.name());
    assertEquals("UnityEngine.MonoBehaviour", mover.typeAttrs().baseType().toString());
    assertEquals(pos(3, 1), mover.span().toString());

    FieldDecl speed = assertInstanceOf(FieldDecl.class, mover.members().get(0));
    assertEquals(List.of(Modifier.PUBLIC), speed.modifiers());
    assertEquals(pos(4, 3), speed.span().toString());
    NumberLit init = assertInstanceOf(NumberLit.class, speed.initializer());
    assertEquals(pos(4, 41), init.span().toString());

    MethodDecl update = assertInstanceOf(MethodDecl.class, mover.members().get(1));
    assertEquals("Update", update.name());
    assertEquals(List.of(Modifier.PRIVATE), update.modifiers());
    assertEquals(pos(5, 3), update.span().toString());
    BlockStmt body = assertInstanceOf(BlockStmt.class, update.body());
    assertEquals(pos(6, 5), body.span().toString());

    checkLocal(assertInstanceOf(LocalStmt.class, body.statements().get(0)));
    checkTranslateCall(assertInstanceOf(ExprStmt.class, body.statements().get(1)));
  }

  private static void checkLocal(LocalStmt local) {
    assertEquals(pos(7, 7), local.span().toString());
    StaticCallExpr getAxis = assertInstanceOf(StaticCallExpr.class, local.initializer());
    assertEquals("UnityEngine.Input::GetAxis#string", getAxis.method().toString());
    assertEquals(pos(7, 20), getAxis.span().toString());
    StringLit horizontal = assertInstanceOf(StringLit.class, getAxis.arguments().get(0));
    assertEquals(pos(7, 61), horizontal.span().toString());
  }

  private static void checkTranslateCall(ExprStmt stmt) {
    assertEquals(pos(8, 7), stmt.span().toString());
    CallExpr translate = assertInstanceOf(CallExpr.class, stmt.expression());
    assertEquals(pos(9, 9), translate.span().toString());
    PropExpr transform = assertInstanceOf(PropExpr.class, translate.target());
    assertEquals(pos(9, 15), transform.span().toString());
    assertInstanceOf(ThisExpr.class, transform.target());

    StaticCallExpr outer = assertInstanceOf(StaticCallExpr.class, translate.arguments().get(0));
    assertEquals(pos(11, 15), outer.span().toString());
    StaticCallExpr inner = assertInstanceOf(StaticCallExpr.class, outer.arguments().get(0));
    NewExpr vector = assertInstanceOf(NewExpr.class, inner.arguments().get(0));
    assertEquals(pos(13, 19), vector.span().toString());
    IdentExpr h = assertInstanceOf(IdentExpr.class, vector.arguments().get(0));
    assertEquals(pos(13, 63), h.span().toString());
    FieldExpr speed = assertInstanceOf(FieldExpr.class, inner.arguments().get(1));
    assertEquals(pos(14, 19), speed.span().toString());
    StaticPropExpr deltaTime = assertInstanceOf(StaticPropExpr.class, outer.arguments().get(1));
    assertEquals(pos(15, 17), deltaTime.span().toString());
  }

  @Test
  void readsHeaderOnlyFile() throws IrSyntaxException {
    IrFile file = read("(ir :version 1 :assembly \"A\")");
    assertNull(file.header().sha256());
    assertTrue(file.types().isEmpty());
    assertEquals("s.ir", file.fileName());
  }

  @Test
  void rejectsEmptyFile() {
    IrSyntaxException e = rejects(() -> read("  ; leer\n"), "Dateikopf");
    assertEquals("s.ir:1:1", e.span().toString());
  }

  @Test
  void rejectsMissingHeader() {
    IrSyntaxException e = rejects(() -> read("\n(class A)"), "Dateikopf (ir ...) erwartet");
    assertEquals("s.ir:2:1", e.span().toString());
  }

  @Test
  void rejectsUnsupportedVersion() {
    IrSyntaxException e =
        rejects(() -> read("(ir :version 2 :assembly \"A\")"), "nicht unterstützt");
    assertEquals("s.ir:1:14", e.span().toString());
  }

  @Test
  void rejectsSuffixedVersion() {
    rejects(() -> read("(ir :version 1i32 :assembly \"A\")"), "Zahl ohne Suffix");
  }

  @Test
  void rejectsMissingAssembly() {
    rejects(() -> read("(ir :version 1)"), "braucht ':assembly'");
  }

  @Test
  void rejectsPositionalArgumentsInHeader() {
    rejects(() -> read("(ir x :version 1 :assembly \"A\")"), "falsche Stelligkeit");
  }

  @Test
  void rejectsUnknownTopLevelNode() {
    IrSyntaxException e = rejects(() -> read(HEADER + "(module M)"), "Typdeklaration erwartet");
    assertEquals("s.ir:2:1", e.span().toString());
  }

  @Test
  void ignoresUnknownKeywordInHeader() throws IrSyntaxException {
    IrFile file = read("(ir :version 1 :tool \"x\" :assembly \"A\" :future (1 2))");
    assertEquals("A", file.header().assembly());
  }

  @Test
  void readsMultipleTypesInInputOrder() throws IrSyntaxException {
    IrFile file = read(HEADER + "(class B)\n(enum A :underlying i32)\n(struct C)");
    assertEquals(3, file.types().size());
    assertEquals("B", file.types().get(0).name());
    assertEquals("A", file.types().get(1).name());
    assertEquals("s.ir:4:1", file.types().get(2).span().toString());
  }

  @Test
  void readsNodeAttributesOnDeclarations() throws IrSyntaxException {
    IrFile file =
        read(HEADER + "(class A (field f i32 :manual \"Grund\" :loc \"A.cs:1\") :note \"n\")");
    ClassDecl clazz = (ClassDecl) file.types().get(0);
    assertEquals("n", clazz.attrs().note());
    FieldDecl field = (FieldDecl) clazz.members().get(0);
    assertEquals("Grund", field.attrs().manual());
    assertEquals("A.cs:1", field.attrs().loc());
  }
}
