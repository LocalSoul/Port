package io.github.localsoul.port.ir.reader;

import static io.github.localsoul.port.ir.reader.Snippets.rejects;
import static io.github.localsoul.port.ir.reader.Snippets.type;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.InterfaceDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Modifier;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.ParamMode;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.StructDecl;
import io.github.localsoul.port.ir.ast.TypeAttrs;
import io.github.localsoul.port.ir.ast.TypeDecl;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

class DeclarationReaderTest {

  @Test
  void readsClassWithAllTypeAttributes() throws IrSyntaxException {
    TypeDecl decl =
        type(
            "(class Ns.A :base B :implements (I J<i32>) :generics (T U)"
                + " :mods (public sealed) :attrs ((Ns.Attr 1i32 \"x\") (Ns.Other)))");
    ClassDecl clazz = assertInstanceOf(ClassDecl.class, decl);
    assertEquals("Ns.A", clazz.name());
    TypeAttrs attrs = clazz.typeAttrs();
    assertEquals("B", attrs.baseType().toString());
    assertEquals("[I, J<i32>]", attrs.implementedTypes().toString());
    assertEquals(List.of("T", "U"), attrs.generics());
    assertEquals(List.of(Modifier.PUBLIC, Modifier.SEALED), attrs.modifiers());
    assertEquals(2, attrs.attributes().size());
    assertEquals("Ns.Attr", attrs.attributes().get(0).type());
    assertEquals(2, attrs.attributes().get(0).arguments().size());
    assertEquals("s.ir:2:1", clazz.span().toString());
  }

  @Test
  void readsMinimalTypes() throws IrSyntaxException {
    ClassDecl clazz = assertInstanceOf(ClassDecl.class, type("(class A)"));
    assertNull(clazz.typeAttrs().baseType());
    assertTrue(clazz.members().isEmpty());
    assertInstanceOf(StructDecl.class, type("(struct S)"));
    InterfaceDecl iface =
        assertInstanceOf(InterfaceDecl.class, type("(interface I (method M void () :abstract))"));
    MethodDecl method = assertInstanceOf(MethodDecl.class, iface.members().get(0));
    assertNull(method.body());
  }

  @Test
  void readsEnum() throws IrSyntaxException {
    EnumDecl decl =
        assertInstanceOf(EnumDecl.class, type("(enum E :underlying u8\n  (A 0u8)\n  (B 255u8))"));
    assertEquals("u8", decl.underlyingType().toString());
    assertEquals(2, decl.members().size());
    assertEquals("B", decl.members().get(1).name());
    assertEquals("255u8", decl.members().get(1).value().text());
    assertEquals("s.ir:4:3", decl.members().get(1).span().toString());
  }

  @Test
  void readsField() throws IrSyntaxException {
    ClassDecl clazz =
        (ClassDecl)
            type(
                "(class A (field f i32 :mods (private) :attrs ((UnityEngine.SerializeField))"
                    + " :init 1i32))");
    FieldDecl field = assertInstanceOf(FieldDecl.class, clazz.members().get(0));
    assertEquals("f", field.name());
    assertEquals(List.of(Modifier.PRIVATE), field.modifiers());
    assertEquals("UnityEngine.SerializeField", field.attributes().get(0).type());
    assertNotNull(field.initializer());
  }

  @Test
  void readsProperty() throws IrSyntaxException {
    ClassDecl clazz =
        (ClassDecl) type("(class A (prop P i32 :get (return 1i32) :set (block)) (prop Q i32))");
    PropDecl p = assertInstanceOf(PropDecl.class, clazz.members().get(0));
    assertNotNull(p.getter());
    assertNotNull(p.setter());
    PropDecl q = assertInstanceOf(PropDecl.class, clazz.members().get(1));
    assertNull(q.getter());
  }

  @Test
  void readsMethodWithParameters() throws IrSyntaxException {
    ClassDecl clazz =
        (ClassDecl)
            type(
                "(class A (method M T ((x T :ref) (y i32 :default 0i32) (z f32 :out))"
                    + " :mods (public static) :generics (T) (return x)))");
    MethodDecl method = assertInstanceOf(MethodDecl.class, clazz.members().get(0));
    assertEquals("T", method.returnType().toString());
    assertEquals(List.of("T"), method.generics());
    List<Param> params = method.parameters();
    assertEquals(ParamMode.REF, params.get(0).mode());
    assertNotNull(params.get(1).defaultValue());
    assertEquals(ParamMode.OUT, params.get(2).mode());
  }

  @Test
  void readsConstructors() throws IrSyntaxException {
    ClassDecl clazz =
        (ClassDecl) type("(class A (ctor () :base-call (1i32) (block)) (ctor ((a i32)) (block)))");
    CtorDecl withBase = assertInstanceOf(CtorDecl.class, clazz.members().get(0));
    assertEquals(1, withBase.baseCallArguments().size());
    CtorDecl plain = assertInstanceOf(CtorDecl.class, clazz.members().get(1));
    assertNull(plain.baseCallArguments());
  }

  @Test
  void readsEvent() throws IrSyntaxException {
    ClassDecl clazz = (ClassDecl) type("(class A (event E System.Action :mods (public)))");
    EventDecl event = assertInstanceOf(EventDecl.class, clazz.members().get(0));
    assertEquals("System.Action", event.type().toString());
  }

  @Test
  void ignoresUnknownKeywordsWithValues() throws IrSyntaxException {
    ClassDecl clazz =
        (ClassDecl)
            type("(class A :future (a b) (field f i32 :whatever 5i32 :mods (public)) :flag)");
    FieldDecl field = assertInstanceOf(FieldDecl.class, clazz.members().get(0));
    assertEquals(List.of(Modifier.PUBLIC), field.modifiers());
    assertEquals(1, clazz.members().size());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "(foo A)                                         | Typdeklaration erwartet",
        "x                                               | Liste (Typdeklaration)",
        "(class)                                         | falsche Stelligkeit",
        "(class 5i32)                                    | qualifizierter Name",
        "(class A.)                                      | qualifizierter Name",
        "(class A :base 5i32)                            | Typ erwartet",
        "(class A :implements I)                         | Liste",
        "(class A :mods (public weird))                  | unbekannter Modifier",
        "(class A :generics (1i32))                      | Bezeichner",
        "(class A :attrs (x))                            | Liste",
        "(class A :attrs ((1i32)))                       | Knotenname erwartet",
        "(class A (block))                               | Mitglied erwartet",
        "(class A x)                                     | Liste (Mitglied)",
        "(struct)                                        | falsche Stelligkeit",
        "(interface 1i32)                                | qualifizierter Name",
        "(enum E (A 0i32))                               | braucht ':underlying'",
        "(enum E :underlying i32 (A 0))                  | Zahl mit Typsuffix",
        "(enum E :underlying i32 (A 300u8))              | außerhalb des Bereichs",
        "(enum E :underlying i32 (A))                    | falsche Stelligkeit",
        "(enum E :underlying i32 (A.B 1i32))             | Bezeichner",
        "(enum E :underlying 1i32)                       | Typ erwartet",
        "(class A (field f))                             | falsche Stelligkeit",
        "(class A (field 1i32 i32))                      | Bezeichner",
        "(class A (field f i32 :init))                   | ohne Wert",
        "(class A (field f i32 :init :mods (public)))    | ohne Wert",
        "(class A (field f i32 :mods (public) :mods ())) | doppelt",
        "(class A (field f 1i32))                        | Typ erwartet",
        "(class A (prop P))                              | falsche Stelligkeit",
        "(class A (prop P i32 :get 5i32))                | Statement",
        "(class A (method M void))                       | falsche Stelligkeit",
        "(class A (method M void ()))                    | braucht Rumpf oder :abstract",
        "(class A (method M void () :abstract (block)))  | Rumpf und :abstract",
        "(class A (method M void (x) (block)))           | Parameter",
        "(class A (method M void ((x)) (block)))         | falsche Stelligkeit",
        "(class A (method M void ((x i32 :ref :out)) (block))) | nur eines",
        "(class A (method M.N void () (block)))          | Bezeichner",
        "(class A (method M void () :generics T (block))) | Liste",
        "(class A (ctor (block)))                        | falsche Stelligkeit",
        "(class A (ctor () :base-call x (block)))        | Liste",
        "(class A (event E))                             | falsche Stelligkeit",
        "(class A (event E System.Action :mods public))  | Liste",
        "(class A (method M void () :native (:arch x86-64 :abi sysv64) (cfg ()))) | nativer Dialekt"
      })
  void rejectsInvalidDeclaration(String text, String reason) {
    rejects(() -> type(text.replace('~', '"')), reason);
  }
}
