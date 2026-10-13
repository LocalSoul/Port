package io.github.localsoul.port.ir.reader;

import static io.github.localsoul.port.ir.reader.Snippets.expr;
import static io.github.localsoul.port.ir.reader.Snippets.rejects;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.BinaryExpr;
import io.github.localsoul.port.ir.ast.BinaryOp;
import io.github.localsoul.port.ir.ast.CallExpr;
import io.github.localsoul.port.ir.ast.CharLit;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.IdentExpr;
import io.github.localsoul.port.ir.ast.IncDecExpr;
import io.github.localsoul.port.ir.ast.IncDecOp;
import io.github.localsoul.port.ir.ast.IsExpr;
import io.github.localsoul.port.ir.ast.LambdaExpr;
import io.github.localsoul.port.ir.ast.NumberLit;
import io.github.localsoul.port.ir.ast.ParamMode;
import io.github.localsoul.port.ir.ast.RawLanguage;
import io.github.localsoul.port.ir.ast.RawNode;
import io.github.localsoul.port.ir.ast.SetOpExpr;
import io.github.localsoul.port.ir.sexpr.NumberSuffix;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class ExpressionReaderTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "5i32                                 | NumberLit",
        "-128i8                               | NumberLit",
        "255u8                                | NumberLit",
        "0xFFFFFFFFi32                        | NumberLit",
        "18446744073709551615u64              | NumberLit",
        "1.5e10f32                            | NumberLit",
        "~text~                           | StringLit",
        "true                                 | BoolLit",
        "false                                | BoolLit",
        "null                                 | NullLit",
        "(char 97)                            | CharLit",
        "x                                    | IdentExpr",
        "this                                 | ThisExpr",
        "base                                 | BaseExpr",
        "(field this A::f)                    | FieldExpr",
        "(sfield A::f)                        | StaticFieldExpr",
        "(prop this A::p)                     | PropExpr",
        "(sprop A::p)                         | StaticPropExpr",
        "(call this A::m#i32 1i32)            | CallExpr",
        "(call this A::m#)                    | CallExpr",
        "(scall A::m#)                        | StaticCallExpr",
        "(new A::.ctor#f32 1.0f32)            | NewExpr",
        "(new-array i32 3i32)                 | NewArrayExpr",
        "(new-array f32 2i32 3i32)            | NewArrayExpr",
        "(array-lit i32 1i32 2i32)            | ArrayLitExpr",
        "(array-lit i32)                      | ArrayLitExpr",
        "(index a 0i32)                       | IndexExpr",
        "(index a 0i32 1i32)                  | IndexExpr",
        "(+ 1i32 2i32)                        | BinaryExpr",
        "(<< a 2i32)                          | BinaryExpr",
        "(&& a b)                             | BinaryExpr",
        "(neg x)                              | UnaryExpr",
        "(not x)                              | UnaryExpr",
        "(bnot x)                             | UnaryExpr",
        "(checked (+ a b))                    | CheckedExpr",
        "(unchecked a)                        | UncheckedExpr",
        "(set x 1i32)                         | SetExpr",
        "(set (index a 0i32) 1i32)            | SetExpr",
        "(set (sprop A::p) 1i32)              | SetExpr",
        "(set-op + x 1i32)                    | SetOpExpr",
        "(pre-inc x)                          | IncDecExpr",
        "(post-dec (field this A::f))         | IncDecExpr",
        "(conv f32 x)                         | ConvExpr",
        "(cast A x)                           | CastExpr",
        "(as A x)                             | AsExpr",
        "(is A x)                             | IsExpr",
        "(is A x y)                           | IsExpr",
        "(cond c a b)                         | CondExpr",
        "(coalesce a b)                       | CoalesceExpr",
        "(typeof A)                           | TypeofExpr",
        "(default i32)                        | DefaultExpr",
        "(lambda ((a i32)) (return a))        | LambdaExpr",
        "(delegate this A::m#)                | DelegateExpr",
        "(event-add this A::e h)              | EventAddExpr",
        "(event-remove this A::e h)           | EventRemoveExpr",
        "(copy v)                             | CopyExpr",
        "(raw cs ~a ?. b~)                | RawNode",
        "(manual ~Grund~)                 | ManualNode"
      })
  void readsExpression(String text, String expectedClass) throws IrSyntaxException {
    Expr expr = expr(text.replace('~', '"'));
    assertEquals(expectedClass, expr.getClass().getSimpleName());
    assertEquals("s.ir:4:1", expr.span().toString());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "5                          | Zahl ohne Typsuffix",
        "5.0                        | ohne Typsuffix",
        "300u8                      | außerhalb des Bereichs",
        "128i8                      | außerhalb des Bereichs",
        "-129i8                     | außerhalb des Bereichs",
        "-1u32                      | negativer Wert",
        "0x1FFu8                    | außerhalb des Bereichs",
        "1e40f32                    | außerhalb des Bereichs",
        "(char 70000)               | char-Wert",
        "(char 97u16)               | Zahl ohne Suffix",
        "(char)                     | falsche Stelligkeit",
        "Foo.Bar                    | Ausdruck",
        "(field this)               | falsche Stelligkeit",
        "(field this 5i32)          | Member-Referenz erwartet",
        "(sfield)                   | falsche Stelligkeit",
        "(sfield A::f A::g)         | falsche Stelligkeit",
        "(prop this)                | falsche Stelligkeit",
        "(sprop ~x~)            | Member-Referenz erwartet",
        "(call this)                | falsche Stelligkeit",
        "(call this A)              | '::' erwartet",
        "(scall)                    | falsche Stelligkeit",
        "(scall 1i32)               | Member-Referenz erwartet",
        "(new)                      | falsche Stelligkeit",
        "(new A::m#)                | .ctor",
        "(new-array i32)            | falsche Stelligkeit",
        "(new-array 5i32 1i32)      | Typ erwartet",
        "(array-lit)                | falsche Stelligkeit",
        "(array-lit (x) 1i32)       | Typ erwartet",
        "(index a)                  | falsche Stelligkeit",
        "(+ 1i32)                   | falsche Stelligkeit",
        "(+ 1i32 2i32 3i32)         | falsche Stelligkeit",
        "(neg)                      | falsche Stelligkeit",
        "(not a b)                  | falsche Stelligkeit",
        "(checked)                  | falsche Stelligkeit",
        "(unchecked a b)            | falsche Stelligkeit",
        "(set x)                    | falsche Stelligkeit",
        "(set 1i32 2i32)            | L-Wert",
        "(set (call this A::m#) x)  | L-Wert",
        "(set-op foo x 1i32)        | Operator",
        "(set-op + 1i32 1i32)       | L-Wert",
        "(set-op + x)               | falsche Stelligkeit",
        "(post-inc 5i32)            | L-Wert",
        "(pre-dec)                  | falsche Stelligkeit",
        "(conv f32)                 | falsche Stelligkeit",
        "(conv 1i32 x)              | Typ erwartet",
        "(cast A)                   | falsche Stelligkeit",
        "(as (A) x)                 | Typ erwartet",
        "(is A)                     | falsche Stelligkeit",
        "(is A x 5i32)              | Bezeichner",
        "(cond a b)                 | falsche Stelligkeit",
        "(coalesce a)               | falsche Stelligkeit",
        "(typeof)                   | falsche Stelligkeit",
        "(default i32 x)            | falsche Stelligkeit",
        "(default 0i32)             | Typ erwartet",
        "(lambda (a) (return))      | Parameter",
        "(lambda () x)              | Statement",
        "(lambda ())                | falsche Stelligkeit",
        "(delegate this)            | falsche Stelligkeit",
        "(delegate this x)          | '::' erwartet",
        "(event-add this A::e)      | falsche Stelligkeit",
        "(event-remove this 1i32 h) | Member-Referenz erwartet",
        "(copy)                     | falsche Stelligkeit",
        "(raw js ~x~)           | Sprache",
        "(raw cs x)                 | String",
        "(raw cs)                   | falsche Stelligkeit",
        "(manual)                   | falsche Stelligkeit",
        "(manual x)                 | String",
        "(block)                    | ist ein Statement",
        "(foo x)                    | unbekannter Knotenname",
        "(load i32 p)               | nativer Dialekt",
        "(ncall f i32)              | nativer Dialekt",
        "()                         | leere Liste",
        "(~x~ y)                | Knotenname erwartet"
      })
  void rejectsInvalidExpression(String text, String reason) {
    rejects(() -> expr(text.replace('~', '"')), reason);
  }

  @Test
  void reportsErrorPositionOfInnerNode() {
    IrSyntaxException e = rejects(() -> expr("(+ a\n  (field this))"), "falsche Stelligkeit");
    assertEquals("s.ir:5:3", e.span().toString());
  }

  @Test
  void readsNumberDetails() throws IrSyntaxException {
    NumberLit number = assertInstanceOf(NumberLit.class, expr("0x7FFE0000u64"));
    assertEquals("0x7FFE0000u64", number.text());
    assertEquals(NumberSuffix.U64, number.suffix());
  }

  @Test
  void readsCharValue() throws IrSyntaxException {
    CharLit c = assertInstanceOf(CharLit.class, expr("(char 65535)"));
    assertEquals('￿', c.value());
  }

  @Test
  void readsCallDetails() throws IrSyntaxException {
    CallExpr call = assertInstanceOf(CallExpr.class, expr("(call this A::m#i32,f32 1i32 x)"));
    assertEquals("A::m#i32,f32", call.method().toString());
    assertEquals(2, call.arguments().size());
    IdentExpr x = assertInstanceOf(IdentExpr.class, call.arguments().get(1));
    assertEquals("x", x.name());
    assertEquals("s.ir:4:30", x.span().toString());
  }

  @Test
  void readsOperators() throws IrSyntaxException {
    BinaryExpr binary = assertInstanceOf(BinaryExpr.class, expr("(>= a b)"));
    assertEquals(BinaryOp.GE, binary.op());
    SetOpExpr setOp = assertInstanceOf(SetOpExpr.class, expr("(set-op | x 1i32)"));
    assertEquals(BinaryOp.OR, setOp.op());
    IncDecExpr incDec = assertInstanceOf(IncDecExpr.class, expr("(post-inc x)"));
    assertEquals(IncDecOp.POST_INC, incDec.op());
  }

  @Test
  void readsIsBinding() throws IrSyntaxException {
    assertNull(assertInstanceOf(IsExpr.class, expr("(is A x)")).binding());
    assertEquals("y", assertInstanceOf(IsExpr.class, expr("(is A x y)")).binding());
  }

  @Test
  void readsLambdaParameters() throws IrSyntaxException {
    LambdaExpr lambda =
        assertInstanceOf(LambdaExpr.class, expr("(lambda ((a i32 :ref) (b f32)) (return))"));
    assertEquals(ParamMode.REF, lambda.parameters().get(0).mode());
    assertEquals(ParamMode.VALUE, lambda.parameters().get(1).mode());
  }

  @Test
  void readsRawText() throws IrSyntaxException {
    RawNode raw = assertInstanceOf(RawNode.class, expr("(raw asm \"mov eax, 1\")"));
    assertEquals(RawLanguage.ASM, raw.language());
    assertEquals("mov eax, 1", raw.text());
  }

  @Test
  void readsNodeAttributesOnExpressions() throws IrSyntaxException {
    Expr expr = expr("(+ a b :loc \"A.cs:3\" :note \"n\")");
    assertEquals("A.cs:3", expr.attrs().loc());
    assertNull(expr.attrs().manual());
    assertEquals("n", expr.attrs().note());
  }
}
