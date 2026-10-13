package io.github.localsoul.port.ir.reader;

import static io.github.localsoul.port.ir.reader.Snippets.rejects;
import static io.github.localsoul.port.ir.reader.Snippets.stmt;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.BlockStmt;
import io.github.localsoul.port.ir.ast.ForStmt;
import io.github.localsoul.port.ir.ast.IfStmt;
import io.github.localsoul.port.ir.ast.LocalStmt;
import io.github.localsoul.port.ir.ast.ReturnStmt;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.SwitchStmt;
import io.github.localsoul.port.ir.ast.TryStmt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class StatementReaderTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "(block)                                                    | BlockStmt",
        "(block (break) (continue))                                 | BlockStmt",
        "(local x i32)                                              | LocalStmt",
        "(local x i32 1i32)                                         | LocalStmt",
        "(expr (set x 1i32))                                        | ExprStmt",
        "(if c (block))                                             | IfStmt",
        "(if c (block) (block))                                     | IfStmt",
        "(while c (block))                                          | WhileStmt",
        "(do-while (block) c)                                       | DoWhileStmt",
        "(for ((local i i32 0i32)) (< i n) ((post-inc i)) (block))  | ForStmt",
        "(for () true () (break))                                   | ForStmt",
        "(foreach x i32 xs (block))                                 | ForeachStmt",
        "(switch x (case (1i32 2i32) (break)) (default (break)))    | SwitchStmt",
        "(switch x)                                                 | SwitchStmt",
        "(break)                                                    | BreakStmt",
        "(continue)                                                 | ContinueStmt",
        "(return)                                                   | ReturnStmt",
        "(return x)                                                 | ReturnStmt",
        "(throw e)                                                  | ThrowStmt",
        "(try (block) (catch e System.Exception (block)) (finally (block))) | TryStmt",
        "(try (block) (finally (block)))                            | TryStmt",
        "(yield-return x)                                           | YieldReturnStmt",
        "(yield-break)                                              | YieldBreakStmt",
        "(lock o (block))                                           | LockStmt",
        "(using r System.IDisposable r0 (block))                    | UsingStmt",
        "(label L1)                                                 | LabelStmt",
        "(goto L1)                                                  | GotoStmt",
        "(raw cs ~x++;~)                                            | RawNode",
        "(manual ~später~)                                          | ManualNode"
      })
  void readsStatement(String text, String expectedClass) throws IrSyntaxException {
    Stmt stmt = stmt(text.replace('~', '"'));
    assertEquals(expectedClass, stmt.getClass().getSimpleName());
    assertEquals("s.ir:3:1", stmt.span().toString());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "x                                       | Statement (Liste) erwartet",
        "(local x)                               | falsche Stelligkeit",
        "(local 5i32 i32)                        | Bezeichner",
        "(local x 5i32)                          | Typ erwartet",
        "(expr)                                  | falsche Stelligkeit",
        "(expr (block))                          | ist ein Statement",
        "(if c)                                  | falsche Stelligkeit",
        "(if c x)                                | Statement (Liste) erwartet",
        "(if c (block) (block) (block))          | falsche Stelligkeit",
        "(while c)                               | falsche Stelligkeit",
        "(do-while c (block))                    | Statement (Liste) erwartet",
        "(do-while (block))                      | falsche Stelligkeit",
        "(for () c ())                           | falsche Stelligkeit",
        "(for x c () (block))                    | Liste",
        "(for () c x (block))                    | Liste",
        "(foreach x i32 xs)                      | falsche Stelligkeit",
        "(foreach 1i32 i32 xs (block))           | Bezeichner",
        "(switch)                                | falsche Stelligkeit",
        "(switch x (case 1i32 (break)))          | Liste",
        "(switch x (case))                       | falsche Stelligkeit",
        "(switch x (default) (case () (break)))  | letzte Zweig",
        "(switch x (foo))                        | (case ...) oder (default ...) erwartet",
        "(switch x y)                            | Liste",
        "(break x)                               | falsche Stelligkeit",
        "(continue x)                            | falsche Stelligkeit",
        "(return a b)                            | falsche Stelligkeit",
        "(throw)                                 | falsche Stelligkeit",
        "(try)                                   | falsche Stelligkeit",
        "(try (block) (finally (block)) (catch e E (block))) | letzte Zweig",
        "(try (block) (catch e E))               | falsche Stelligkeit",
        "(try (block) (catch 1i32 E (block)))    | Bezeichner",
        "(try (block) (finally))                 | falsche Stelligkeit",
        "(try (block) (foo))                     | (catch ...) oder (finally ...) erwartet",
        "(yield-return)                          | falsche Stelligkeit",
        "(yield-break x)                         | falsche Stelligkeit",
        "(lock o)                                | falsche Stelligkeit",
        "(using r T x)                           | falsche Stelligkeit",
        "(using r 1i32 x (block))                | Typ erwartet",
        "(label)                                 | falsche Stelligkeit",
        "(goto 5i32)                             | Bezeichner",
        "(call this A::m#)                       | ist ein Ausdruck",
        "(+ a b)                                 | ist ein Ausdruck",
        "(cfg ())                                | nativer Dialekt",
        "(br L)                                  | nativer Dialekt",
        "(store i32 p v)                         | nativer Dialekt",
        "(whatever)                              | unbekannter Knotenname"
      })
  void rejectsInvalidStatement(String text, String reason) {
    rejects(() -> stmt(text.replace('~', '"')), reason);
  }

  @Test
  void readsOptionalPartsAsNull() throws IrSyntaxException {
    assertNull(assertInstanceOf(IfStmt.class, stmt("(if c (block))")).elseBranch());
    assertNull(assertInstanceOf(LocalStmt.class, stmt("(local x i32)")).initializer());
    assertNull(assertInstanceOf(ReturnStmt.class, stmt("(return)")).value());
    assertNull(assertInstanceOf(SwitchStmt.class, stmt("(switch x)")).defaultCase());
    assertNull(
        assertInstanceOf(TryStmt.class, stmt("(try (block) (catch e E (block)))")).finallyClause());
  }

  @Test
  void readsSwitchBranches() throws IrSyntaxException {
    SwitchStmt sw =
        assertInstanceOf(
            SwitchStmt.class,
            stmt("(switch x\n  (case (1i32 2i32) (expr y) (break))\n  (default (return)))"));
    assertEquals(1, sw.cases().size());
    assertEquals(2, sw.cases().get(0).labels().size());
    assertEquals(2, sw.cases().get(0).body().size());
    assertEquals("s.ir:4:3", sw.cases().get(0).span().toString());
    assertNotNull(sw.defaultCase());
    assertEquals("s.ir:5:3", sw.defaultCase().span().toString());
  }

  @Test
  void readsForParts() throws IrSyntaxException {
    ForStmt loop =
        assertInstanceOf(
            ForStmt.class, stmt("(for ((local i i32 0i32)) (< i n) ((post-inc i)) (block))"));
    assertEquals(1, loop.initializers().size());
    assertEquals(1, loop.updates().size());
  }

  @Test
  void keepsPositionsAcrossLinesAndComments() throws IrSyntaxException {
    BlockStmt block =
        assertInstanceOf(
            BlockStmt.class, stmt("(block ; Kommentar (break)\n  ; ganze Zeile\n    (continue))"));
    assertEquals(1, block.statements().size());
    assertEquals("s.ir:5:5", block.statements().get(0).span().toString());
  }

  @Test
  void readsNodeAttributesOnStatements() throws IrSyntaxException {
    Stmt stmt = stmt("(break :manual \"Grund\" :loc \"A.cs:9\")");
    assertEquals("Grund", stmt.attrs().manual());
    assertEquals("A.cs:9", stmt.attrs().loc());
  }

  @Test
  void rejectsNonStringNodeAttribute() {
    rejects(() -> stmt("(break :loc 5i32)"), "':loc' erwartet einen String");
  }

  @Test
  void rejectsDuplicateNodeAttribute() {
    rejects(() -> stmt("(break :note \"a\" :note \"b\")"), "doppelt");
  }
}
