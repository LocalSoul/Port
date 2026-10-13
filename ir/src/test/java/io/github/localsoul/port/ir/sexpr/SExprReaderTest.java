package io.github.localsoul.port.ir.sexpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import java.util.List;
import org.junit.jupiter.api.Test;

class SExprReaderTest {

  private static final String SPEC_EXAMPLE =
      """
      (ir :version 1 :assembly "Assembly-CSharp" :sha256 "ab12...")

      (class Mover :base UnityEngine.MonoBehaviour
        (field speed f32 :mods (public) :init 5.0f32)
        (method Update void () :mods (private)
          (block
            (local h f32 (scall UnityEngine.Input::GetAxis#string "Horizontal"))
            (expr
              (call (prop this UnityEngine.Component::transform)
                    UnityEngine.Transform::Translate#UnityEngine.Vector3
                    (scall UnityEngine.Vector3::op_Multiply#UnityEngine.Vector3,f32
                      (scall UnityEngine.Vector3::op_Multiply#UnityEngine.Vector3,f32
                        (new UnityEngine.Vector3::.ctor#f32,f32,f32 h 0.0f32 0.0f32)
                        (field this Mover::speed))
                      (sprop UnityEngine.Time::deltaTime)))))))
      """;

  @Test
  void readsSpecExampleWithPositions() throws IrSyntaxException {
    List<SExpr> exprs = SExprReader.readAll(SPEC_EXAMPLE, "Mover.ir");
    assertEquals(2, exprs.size());

    SList header = assertInstanceOf(SList.class, exprs.get(0));
    assertEquals("Mover.ir:1:1", header.span().toString());
    assertEquals(7, header.items().size());
    assertInstanceOf(SBareInteger.class, header.items().get(2));
    SString assembly = assertInstanceOf(SString.class, header.items().get(4));
    assertEquals("Assembly-CSharp", assembly.value());

    SList clazz = assertInstanceOf(SList.class, exprs.get(1));
    assertEquals("Mover.ir:3:1", clazz.span().toString());
    SList field = assertInstanceOf(SList.class, clazz.items().get(4));
    assertEquals("Mover.ir:4:3", field.span().toString());
    SNumber init = assertInstanceOf(SNumber.class, field.items().get(6));
    assertEquals("5.0f32", init.text());
    assertEquals(NumberSuffix.F32, init.suffix());
    assertEquals("Mover.ir:4:41", init.span().toString());

    SList method = assertInstanceOf(SList.class, clazz.items().get(5));
    SList params = assertInstanceOf(SList.class, method.items().get(3));
    assertTrue(params.items().isEmpty());
    assertEquals("Mover.ir:5:23", params.span().toString());
    SKeyword mods = assertInstanceOf(SKeyword.class, method.items().get(4));
    assertEquals("mods", mods.name());
  }

  @Test
  void readsEmptyInput() throws IrSyntaxException {
    assertTrue(SExprReader.readAll("", "e.ir").isEmpty());
    assertTrue(SExprReader.readAll(" ; nichts\n", "e.ir").isEmpty());
  }

  @Test
  void readsNestedLists() throws IrSyntaxException {
    List<SExpr> exprs = SExprReader.readAll("(a (b (c)) ())", "n.ir");
    SList outer = assertInstanceOf(SList.class, exprs.get(0));
    assertEquals(3, outer.items().size());
    SList b = assertInstanceOf(SList.class, outer.items().get(1));
    SList c = assertInstanceOf(SList.class, b.items().get(1));
    SSymbol cSymbol = assertInstanceOf(SSymbol.class, c.items().get(0));
    assertEquals("c", cSymbol.text());
    assertEquals("n.ir:1:7", c.span().toString());
  }

  @Test
  void unclosedParenthesisReportsOpeningPosition() {
    String text = "(class A\n  (field x i32\n  (method M void () (block))";
    IrSyntaxException e =
        assertThrows(IrSyntaxException.class, () -> SExprReader.readAll(text, "u.ir"));
    assertEquals("u.ir:2:3", e.span().toString());
    assertTrue(e.reason().contains("nicht geschlossen"), e.reason());
  }

  @Test
  void unclosedOuterParenthesisReportsItsPosition() {
    IrSyntaxException e =
        assertThrows(IrSyntaxException.class, () -> SExprReader.readAll("\n  (a (b)", "u.ir"));
    assertEquals("u.ir:2:3", e.span().toString());
  }

  @Test
  void rejectsUnexpectedClosingParenthesis() {
    IrSyntaxException e =
        assertThrows(IrSyntaxException.class, () -> SExprReader.readAll("(a)\n )", "x.ir"));
    assertEquals("x.ir:2:2", e.span().toString());
    assertTrue(e.reason().contains("schließende Klammer"), e.reason());
  }

  @Test
  void propagatesLexerErrorsWithPosition() {
    IrSyntaxException e =
        assertThrows(IrSyntaxException.class, () -> SExprReader.readAll("(a\n (b 5.0))", "x.ir"));
    assertEquals("x.ir:2:5", e.span().toString());
  }
}
