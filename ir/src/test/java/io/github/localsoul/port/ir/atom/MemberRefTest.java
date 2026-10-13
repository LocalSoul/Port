package io.github.localsoul.port.ir.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class MemberRefTest {

  private static final SourceSpan AT = new SourceSpan("m.ir", 7, 1);

  private static MemberRef parse(String text) throws IrSyntaxException {
    return MemberRef.parse(text, AT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "UnityEngine.Transform::Translate#UnityEngine.Vector3",
        "Foo::.ctor#f32,f32,f32",
        "Foo::Bar#",
        "Foo::.cctor#",
        "Mover::speed",
        "UnityEngine.Time::deltaTime",
        "Typ::Name<i32>#string",
        "Typ::Map<K,V>#Dictionary<K,V[]>,i32&",
        "UnityEngine.Vector3::op_Multiply#UnityEngine.Vector3,f32",
        "Native.libmath::clamp_add#u32,u32"
      })
  void parsesAndPrintsIdentically(String text) throws IrSyntaxException {
    MemberRef ref = parse(text);
    assertEquals(text, ref.toString());
  }

  @Test
  void splitsMethodReference() throws IrSyntaxException {
    MemberRef ref = parse("UnityEngine.Transform::Translate#UnityEngine.Vector3");
    assertEquals("UnityEngine.Transform", ref.declaringType());
    assertEquals("Translate", ref.name());
    assertTrue(ref.hasParameterList());
    assertEquals(List.of(new NamedType("UnityEngine.Vector3", List.of())), ref.parameterTypes());
  }

  @Test
  void distinguishesEmptyParameterListFromFieldReference() throws IrSyntaxException {
    MemberRef method = parse("Foo::Bar#");
    MemberRef field = parse("Foo::Bar");
    assertTrue(method.hasParameterList());
    assertTrue(method.parameterTypes().isEmpty());
    assertFalse(field.hasParameterList());
    assertFalse(method.equals(field));
  }

  @Test
  void parsesConstructor() throws IrSyntaxException {
    MemberRef ref = parse("Foo::.ctor#f32,f32,f32");
    assertEquals(".ctor", ref.name());
    assertEquals(3, ref.parameterTypes().size());
  }

  @Test
  void parsesGenericMethodArguments() throws IrSyntaxException {
    MemberRef ref = parse("Typ::Name<i32>#string");
    assertEquals(List.of(new PrimitiveType(Primitive.I32)), ref.genericArguments());
    assertEquals(List.of(new PrimitiveType(Primitive.STRING)), ref.parameterTypes());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "Foo::               | 6  | Member-Name erwartet",
        "Foo                 | 4  | '::' erwartet",
        "Foo:bar             | 4  | '::' erwartet",
        "::bar               | 1  | Typname erwartet",
        "Foo::.dtor          | 6  | Member-Name erwartet",
        "Foo::Bar#i32,       | 14 | Typname erwartet",
        "Foo::Bar#i32[       | 13 | '[' nicht geschlossen",
        "Foo::Bar<>#         | 10 | leere Typargumentliste",
        "Foo::Bar#i32 x      | 13 | unerwartetes ' x'",
        "Foo::Bar##          | 10 | Typname erwartet",
        "Foo::Bar.Baz        | 9  | unerwartetes '.Baz'"
      })
  void rejectsMalformedReferencesWithPositionAndReason(String text, int column, String reason) {
    IrSyntaxException e = assertThrows(IrSyntaxException.class, () -> parse(text));
    assertEquals("m.ir:7:" + column, e.span().toString());
    assertTrue(e.reason().contains(reason), e.reason());
  }

  @Test
  void rejectsParameterTypesWithoutParameterList() {
    List<TypeRef> params = List.of(new PrimitiveType(Primitive.I32));
    assertThrows(
        IllegalArgumentException.class, () -> new MemberRef("Foo", "x", List.of(), false, params));
  }
}
