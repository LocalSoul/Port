package io.github.localsoul.port.ir.validate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.reader.IrReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class IrValidatorTest {

  private static final String HEADER = "(ir :version 1 :assembly \"T\")\n";
  private static final TypeCatalog UNITY_CATALOG =
      Set.of(
              "UnityEngine.MonoBehaviour",
              "UnityEngine.Input",
              "UnityEngine.Component",
              "UnityEngine.Transform",
              "UnityEngine.Vector3",
              "UnityEngine.Time")
          ::contains;

  private static IrFile read(String body) throws IrSyntaxException {
    return new IrReader().read(HEADER + body, "v.ir");
  }

  private static List<Diagnostic> validate(String body) throws IrSyntaxException {
    return new IrValidator().validate(read(body));
  }

  private static List<Diagnostic> validate(String body, TypeCatalog catalog)
      throws IrSyntaxException {
    return new IrValidator(catalog).validate(read(body));
  }

  /** Kurzform {@code ID@zeile:spalte} für Vergleiche. */
  private static List<String> summary(List<Diagnostic> diagnostics) {
    List<String> result = new ArrayList<>();
    for (Diagnostic d : diagnostics) {
      result.add(d.id() + "@" + d.location().line() + ":" + d.location().column());
    }
    return result;
  }

  private static Diagnostic single(List<Diagnostic> diagnostics) {
    assertEquals(1, diagnostics.size(), diagnostics.toString());
    return diagnostics.get(0);
  }

  // --- gültige Dateien ---

  @Test
  void specExampleWithCatalogHasNoDiagnostics() throws IOException, IrSyntaxException {
    Path path = Path.of("..", "testdata", "ir", "spec-example.ir");
    IrFile file = new IrReader().read(Files.readString(path, StandardCharsets.UTF_8), "s.ir");
    assertEquals(List.of(), new IrValidator(UNITY_CATALOG).validate(file));
  }

  @Test
  void selfContainedFileHasNoDiagnostics() throws IrSyntaxException {
    String body =
        """
        (interface Game.IScore (method Get i32 () :abstract))
        (enum Game.Mode :underlying i32 (Easy 0i32) (Hard 1i32))
        (class Game.Board :implements (Game.IScore) :generics (T)
          (field cells T[])
          (field mode Game.Mode :init (sfield Game.Mode::Hard))
          (prop Size i32 :get (return 4i32))
          (ctor ((size i32)) (block))
          (method Get i32 () (return (prop this Game.Board::Size)))
          (method Map U ((value T) (fallback U :ref)) :generics (U) (return fallback))
          (method Make Game.Board () :mods (static)
            (block
              (local b Game.Board (new Game.Board::.ctor#i32 4i32))
              (expr (call b Game.Board::Map<i32>#T,i32& (default T) 0i32))
              (return b))))
        """;
    assertEquals(List.of(), validate(body));
  }

  // --- IR002 ---

  @Test
  void reportsMissingMemberOfTypeInFile() throws IrSyntaxException {
    String body = "(class A (field x i32)\n  (method M void () (return (field this A::y))))";
    Diagnostic d = single(validate(body));
    assertEquals("IR002", d.id());
    assertEquals(Severity.ERROR, d.severity());
    assertEquals("A::M#", d.symbol());
    assertEquals("v.ir:3:29", d.location().toString());
    assertTrue(d.message().contains("A::y"), d.message());
  }

  @Test
  void reportsMethodWithWrongParameterTypes() throws IrSyntaxException {
    String body =
        "(class A (method N void ((a i32)) (return))\n"
            + "  (method M void () (expr (call this A::N#f32 1.0f32))))";
    Diagnostic d = single(validate(body));
    assertEquals("IR002", d.id());
    assertEquals("v.ir:3:27", d.location().toString());
  }

  @Test
  void distinguishesConstructorsAndStaticConstructors() throws IrSyntaxException {
    String body =
        "(class A (ctor () :mods (static) (block))\n"
            + "  (method M void () (block (expr (new A::.ctor#)) (expr (new A::.ctor#i32 1i32)))))";
    List<Diagnostic> diagnostics = validate(body);
    assertEquals(List.of("IR002@3:34", "IR002@3:57"), summary(diagnostics));
  }

  @Test
  void resolvesEnumMembersAndRejectsUnknownOnes() throws IrSyntaxException {
    String body =
        "(enum E :underlying i32 (A 0i32))\n"
            + "(class C (field a E :init (sfield E::A)) (field b E :init (sfield E::B)))";
    Diagnostic d = single(validate(body));
    assertEquals("IR002", d.id());
    assertEquals("C::b", d.symbol());
    assertTrue(d.message().contains("E::B"), d.message());
  }

  @Test
  void reportsExternalTypeNotInCatalogAsError() throws IrSyntaxException {
    String body = "(class A :base UnityEngine.Behaviour)";
    Diagnostic d = single(validate(body, UNITY_CATALOG));
    assertEquals("IR002", d.id());
    assertEquals(Severity.ERROR, d.severity());
    assertEquals("A", d.symbol());
    assertEquals("v.ir:2:1", d.location().toString());
  }

  @Test
  void reportsExternalTypesWithoutCatalogOnceAsInfo() throws IrSyntaxException {
    String body =
        "(class A :base Ext.Base\n  (field a Ext.Thing)\n  (field b Ext.Thing)\n  (field c Ext.Base))";
    List<Diagnostic> diagnostics = validate(body);
    assertEquals(List.of("IR002@2:1", "IR002@3:3"), summary(diagnostics));
    for (Diagnostic d : diagnostics) {
      assertEquals(Severity.INFO, d.severity());
    }
    assertTrue(diagnostics.get(1).message().contains("Ext.Thing"));
  }

  @Test
  void checksTypesInsideGenericsArraysAndSignatures() throws IrSyntaxException {
    String body =
        "(class A (field a List<Missing>[])\n" + "  (method M void () (expr (scall A::N#Gone))))";
    List<Diagnostic> diagnostics = validate(body, Set.of("List")::contains);
    List<String> messages = new ArrayList<>();
    for (Diagnostic d : diagnostics) {
      messages.add(d.message());
    }
    assertEquals(3, diagnostics.size(), messages.toString());
    assertTrue(messages.get(0).contains("Missing"));
    assertTrue(messages.get(1).contains("Gone") || messages.get(2).contains("Gone"));
  }

  @Test
  void doesNotReportGenericTypeParameters() throws IrSyntaxException {
    String body =
        "(class A :generics (T) (field a T) (method M U ((x U)) :generics (U) (return x)))";
    assertEquals(List.of(), validate(body));
  }

  @Test
  void resolvesMembersOfGenericTypeInstancesInFile() throws IrSyntaxException {
    String body =
        "(class Box :generics (T) (field value T) (method Put void ((v T)) (return))"
            + " (method Get T () (return (field this Box<T>::value))))\n"
            + "(class User (method Use void ((b Box<i32>))"
            + " (block (expr (call b Box<i32>::Put#T 1i32)) (expr (field b Box<i32>::value)))))";
    assertEquals(List.of(), validate(body));
  }

  @Test
  void reportsMissingMemberOfGenericTypeInstance() throws IrSyntaxException {
    String body =
        "(class Box :generics (T) (method Put void ((v T)) (return)))\n"
            + "(class User (method Use void ((b Box<i32>)) (expr (call b Box<i32>::Put#i32 1i32))))";
    Diagnostic d = single(validate(body));
    assertEquals("IR002", d.id());
    assertTrue(d.message().contains("Box<i32>::Put#i32"), d.message());
  }

  @Test
  void doesNotReportOpenParameterTypesOfExternalGenericMembers() throws IrSyntaxException {
    String body =
        "(class A (method M void ((xs System.Collections.Generic.List<i32>))"
            + " (expr (call xs System.Collections.Generic.List<i32>::Add#T 1i32))))";
    List<Diagnostic> diagnostics =
        validate(body, Set.of("System.Collections.Generic.List")::contains);
    assertEquals(List.of(), diagnostics);
  }

  @Test
  void checksTypeArgumentsOfDeclaringType() throws IrSyntaxException {
    String body =
        "(class A (method M void () (expr (scall System.Collections.Generic.List<Gone>::Clear#))))";
    Diagnostic d = single(validate(body, Set.of("System.Collections.Generic.List")::contains));
    assertTrue(d.message().contains("'Gone'"), d.message());
  }

  // --- IR003 ---

  @Test
  void reportsUnknownType() throws IrSyntaxException {
    String body = "(class A\n  (field a unknown)\n  (field b List<unknown[]>))";
    List<Diagnostic> diagnostics = validate(body, Set.of("List")::contains);
    assertEquals(List.of("IR003@3:3", "IR003@4:3"), summary(diagnostics));
    assertEquals("A::a", diagnostics.get(0).symbol());
    assertEquals(Severity.ERROR, diagnostics.get(0).severity());
  }

  // --- IR004 ---

  @Test
  void reportsDuplicateMembersAtSecondOccurrence() throws IrSyntaxException {
    String body =
        "(class A\n  (field x i32)\n  (prop x i32)\n  (method M void ((a i32)) (return))\n"
            + "  (method M void ((b i32)) (return))\n  (method M void ((a f32)) (return)))";
    List<Diagnostic> diagnostics = validate(body);
    assertEquals(List.of("IR004@4:3", "IR004@6:3"), summary(diagnostics));
    assertEquals("A::x", diagnostics.get(0).symbol());
    assertEquals("A::M#i32", diagnostics.get(1).symbol());
    assertTrue(diagnostics.get(0).message().contains("v.ir:3:3"), diagnostics.get(0).message());
  }

  @Test
  void reportsDuplicateTypesAndEnumValues() throws IrSyntaxException {
    String body = "(class A)\n(enum A :underlying i32 (X 0i32) (X 1i32))";
    List<Diagnostic> diagnostics = validate(body);
    assertEquals(List.of("IR004@3:1", "IR004@3:34"), summary(diagnostics));
    assertEquals("A", diagnostics.get(0).symbol());
    assertEquals("A::X", diagnostics.get(1).symbol());
  }

  @Test
  void treatsRefParametersAsDifferentOverloads() throws IrSyntaxException {
    String body =
        "(class A (method M void ((a i32)) (return)) (method M void ((a i32 :ref)) (return)))";
    assertEquals(List.of(), validate(body));
  }

  // --- IR005 ---

  @Test
  void reportsEveryTypeOnInheritanceCycle() throws IrSyntaxException {
    String body = "(class A :base B)\n(class B :base C)\n(class C :base A)\n(class D :base A)";
    List<Diagnostic> diagnostics = validate(body);
    assertEquals(List.of("IR005@2:1", "IR005@3:1", "IR005@4:1"), summary(diagnostics));
    assertEquals("A", diagnostics.get(0).symbol());
    assertEquals("Vererbungszyklus: A -> B -> C -> A", diagnostics.get(0).message());
  }

  @Test
  void reportsSelfInheritanceAndInterfaceCycles() throws IrSyntaxException {
    String body = "(class A :base A)\n(interface I :implements (J))\n(interface J :implements (I))";
    List<Diagnostic> diagnostics = validate(body);
    assertEquals(List.of("IR005@2:1", "IR005@3:1", "IR005@4:1"), summary(diagnostics));
    assertEquals("Vererbungszyklus: A -> A", diagnostics.get(0).message());
  }

  @Test
  void acceptsDeepHierarchyWithoutCycle() throws IrSyntaxException {
    String body = "(interface I)\n(class A :implements (I))\n(class B :base A :implements (I))";
    assertEquals(List.of(), validate(body));
  }

  // --- Sammeln und Sortieren ---

  @Test
  void reportsAllProblemsSortedByPositionThenId() throws IrSyntaxException {
    String body =
        "(class B :base A\n"
            + "  (field x unknown)\n"
            + "  (field x i32)\n"
            + "  (method M void () (return (sfield B::nope))))\n"
            + "(class A :base B)\n"
            + "(class Z\n  (field y unknown)\n  (field y unknown))";
    List<Diagnostic> diagnostics = validate(body);
    assertEquals(
        List.of(
            "IR005@2:1",
            "IR003@3:3",
            "IR004@4:3",
            "IR002@5:29",
            "IR005@6:1",
            "IR003@8:3",
            "IR003@9:3",
            "IR004@9:3"),
        summary(diagnostics));
  }

  @Test
  void ordersLinesNumericallyNotAlphabetically() {
    Diagnostic line10 = diagnostic("IR002", 10, 1);
    Diagnostic line9 = diagnostic("IR005", 9, 5);
    Diagnostic line9Earlier = diagnostic("IR004", 9, 1);
    List<Diagnostic> list = new ArrayList<>(List.of(line10, line9, line9Earlier));
    list.sort(Diagnostic.ORDER);
    assertEquals(List.of(line9Earlier, line9, line10), list);
  }

  private static Diagnostic diagnostic(String id, int line, int column) {
    return new Diagnostic(id, Severity.ERROR, "S", "m", new SourceSpan("f.ir", line, column), null);
  }

  // --- IR001 und API ---

  @Test
  void convertsSyntaxErrorToIr001() {
    IrSyntaxException error =
        assertThrows(IrSyntaxException.class, () -> new IrReader().read("(ir", "x.ir"));
    Diagnostic d = IrValidator.syntaxError(error);
    assertEquals("IR001", d.id());
    assertEquals(Severity.ERROR, d.severity());
    assertNull(d.symbol());
    assertEquals("x.ir:1:1", d.location().toString());
  }

  @Test
  void rejectsNullCatalog() {
    assertThrows(IllegalArgumentException.class, () -> new IrValidator(null));
  }

  @Test
  void severitySpellingsMatchContract() {
    assertEquals("error", Severity.ERROR.spelling());
    assertEquals("warn", Severity.WARN.spelling());
    assertEquals("info", Severity.INFO.spelling());
  }
}
