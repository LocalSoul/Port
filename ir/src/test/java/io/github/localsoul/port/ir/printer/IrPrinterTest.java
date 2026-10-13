package io.github.localsoul.port.ir.printer;

import static io.github.localsoul.port.ir.AstAssertions.assertStructurallyEqual;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.ExprStmt;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.NodeAttrs;
import io.github.localsoul.port.ir.ast.NumberLit;
import io.github.localsoul.port.ir.ast.StringLit;
import io.github.localsoul.port.ir.reader.IrReader;
import io.github.localsoul.port.ir.sexpr.NumberSuffix;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class IrPrinterTest {

  private static final Path FIXTURES = Path.of("..", "testdata", "ir");
  private static final Path GOLDEN = Path.of("..", "testdata", "golden", "ir");
  private static final String HEADER = "(ir :version 1 :assembly \"T\")\n";

  private final IrReader reader = new IrReader();
  private final IrPrinter printer = new IrPrinter();

  static List<Path> fixtures() throws IOException {
    List<Path> result = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(FIXTURES, "*.ir")) {
      for (Path path : stream) {
        result.add(path);
      }
    }
    result.sort(null);
    return result;
  }

  private String roundtrip(String text) throws IrSyntaxException {
    return printer.print(reader.read(text, "t.ir"));
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void printIsIdempotent(Path fixture) throws IOException, IrSyntaxException {
    String once = roundtrip(Files.readString(fixture, StandardCharsets.UTF_8));
    String twice = roundtrip(once);
    assertEquals(once, twice);
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void readOfPrintIsStructurallyEqual(Path fixture) throws IOException, IrSyntaxException {
    String canonical = roundtrip(Files.readString(fixture, StandardCharsets.UTF_8));
    IrFile file = reader.read(canonical, "c.ir");
    IrFile again = reader.read(printer.print(file), "c2.ir");
    assertStructurallyEqual(file, again);
  }

  @ParameterizedTest
  @MethodSource("fixtures")
  void matchesGoldenFile(Path fixture) throws IOException, IrSyntaxException {
    String printed = roundtrip(Files.readString(fixture, StandardCharsets.UTF_8));
    Path golden = GOLDEN.resolve(fixture.getFileName());
    if (Boolean.getBoolean("golden.update")) {
      Files.writeString(golden, printed, StandardCharsets.UTF_8);
    }
    assertTrue(
        Files.exists(golden), "Golden File fehlt, mit -Dgolden.update=true erzeugen: " + golden);
    assertEquals(Files.readString(golden, StandardCharsets.UTF_8), printed);
  }

  @Test
  void memberOrderDoesNotChangeOutput() throws IrSyntaxException {
    String a =
        HEADER
            + "(class B (method m void () (return)) (field z i32) (ctor () (block))"
            + " (prop p i32) (field a i32) (event e System.Action))\n"
            + "(class A (method b void () (return)) (method a void ((x i32)) (return))"
            + " (method a void () (return)))";
    String b =
        HEADER
            + "(class A (method a void () (return)) (method a void ((x i32)) (return))"
            + " (method b void () (return)))\n"
            + "(class B (event e System.Action) (field a i32) (prop p i32) (ctor () (block))"
            + " (field z i32) (method m void () (return)))";
    String printedA = roundtrip(a);
    assertEquals(printedA, roundtrip(b));
    String expected =
        HEADER
            + "\n(class A\n"
            + "  (method a void () (return))\n"
            + "  (method a void ((x i32)) (return))\n"
            + "  (method b void () (return)))\n"
            + "\n(class B\n"
            + "  (field a i32)\n"
            + "  (field z i32)\n"
            + "  (prop p i32)\n"
            + "  (event e System.Action)\n"
            + "  (ctor () (block))\n"
            + "  (method m void () (return)))\n";
    assertEquals(expected, printedA);
  }

  @Test
  void endsWithExactlyOneNewline() throws IrSyntaxException {
    String printed = roundtrip(HEADER + "(class A)\n\n\n");
    assertTrue(printed.endsWith(")\n"));
    assertFalse(printed.endsWith("\n\n"));
    assertEquals(HEADER + "\n(class A)\n", printed);
  }

  @Test
  void dropsEmptyOptionalLists() throws IrSyntaxException {
    String printed = roundtrip(HEADER + "(class A :mods () :implements () (field f i32 :mods ()))");
    assertEquals(HEADER + "\n(class A\n  (field f i32))\n", printed);
  }

  @Test
  void keepsEmptyBaseCallDistinctFromMissing() throws IrSyntaxException {
    String printed = roundtrip(HEADER + "(class A (ctor () :base-call () (block)))");
    assertTrue(printed.contains("(ctor () :base-call () (block))"), printed);
  }

  @Test
  void breaksLongListsAndIndentsByTwo() throws IrSyntaxException {
    String longArgs =
        "(scall Some.Very.Long.Namespace.TypeName::MethodWithLongName#string,string,string"
            + " \"first argument\" \"second argument\" \"third argument\")";
    String printed =
        roundtrip(HEADER + "(class A (method m void () (block (expr " + longArgs + "))))");
    String expected =
        "    (block\n"
            + "      (expr\n"
            + "        (scall Some.Very.Long.Namespace.TypeName::MethodWithLongName#string,string,string\n"
            + "          \"first argument\"\n"
            + "          \"second argument\"\n"
            + "          \"third argument\")))))\n";
    assertTrue(printed.endsWith(expected), printed);
  }

  @Test
  void keepsShortListsOnOneLine() throws IrSyntaxException {
    String printed = roundtrip(HEADER + "(class A (method m i32 ((a i32)) (return (+ a 1i32))))");
    assertTrue(printed.contains("  (method m i32 ((a i32)) (return (+ a 1i32))))\n"), printed);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "plain",
        "tab\tnewline\ncr\rquote\"backslash\\",
        "umlaut äöü und emoji \uD83D\uDE00",
        "control \u0001 und del \u007F",
        "lone high \uD800 and lone low \uDC00",
        ""
      })
  void roundtripsStringContent(String value) throws IrSyntaxException {
    String text = StringEscapes.quote(value);
    String printed = roundtrip(HEADER + "(class A (field f string :init " + text + "))");
    IrFile file = reader.read(printed, "s.ir");
    FieldDecl field = (FieldDecl) ((ClassDecl) file.types().get(0)).members().get(0);
    assertEquals(value, ((StringLit) field.initializer()).value());
  }

  @Test
  void escapesControlCharactersAsUnicode() {
    assertEquals("\"a\\u0001b\\u007F\"", StringEscapes.quote("a\u0001b\u007f"));
    assertEquals("\"\\uD800\"", StringEscapes.quote("\uD800"));
  }

  @Test
  void rejectsAttributesOnAtomNodes() throws IrSyntaxException {
    IrFile file = reader.read(HEADER + "(class A (method m void () (expr 1i32)))", "a.ir");
    ClassDecl clazz = (ClassDecl) file.types().get(0);
    MethodDecl method = (MethodDecl) clazz.members().get(0);
    SourceSpan span = method.span();
    NumberLit withNote =
        new NumberLit("1i32", NumberSuffix.I32, new NodeAttrs(null, null, "x"), span);
    ExprStmt stmt = new ExprStmt(withNote, NodeAttrs.NONE, span);
    MethodDecl changed =
        new MethodDecl(
            "m", method.returnType(), List.of(), List.of(), List.of(), stmt, NodeAttrs.NONE, span);
    IrFile changedFile =
        new IrFile(
            "a.ir",
            file.header(),
            List.of(new ClassDecl("A", clazz.typeAttrs(), List.of(changed), NodeAttrs.NONE, span)));
    assertThrows(IllegalArgumentException.class, () -> printer.print(changedFile));
  }
}
