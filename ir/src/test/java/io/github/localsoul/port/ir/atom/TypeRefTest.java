package io.github.localsoul.port.ir.atom;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class TypeRefTest {

  private static final SourceSpan AT = new SourceSpan("t.ir", 3, 10);

  private static TypeRef parse(String text) throws IrSyntaxException {
    return TypeRef.parse(text, AT);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "i32",
        "ptr",
        "string",
        "UnityEngine.Vector3",
        "T",
        "Dictionary<string,i32[]>",
        "f32[,]",
        "f32[,,]",
        "System.Collections.Generic.List<i32>",
        "System.Collections.Generic.Dictionary<string,System.Collections.Generic.List<f32[]>>",
        "i32?",
        "i32?[]",
        "i32[]?",
        "UnityEngine.Vector3&",
        "i32[]&",
        "List<T>[][,]"
      })
  void parsesAndPrintsIdentically(String text) throws IrSyntaxException {
    TypeRef type = parse(text);
    assertEquals(text, type.toString());
  }

  @Test
  void buildsExpectedStructureForGenericWithArrayArgument() throws IrSyntaxException {
    TypeRef type = parse("Dictionary<string,i32[]>");
    NamedType named = assertInstanceOf(NamedType.class, type);
    assertEquals("Dictionary", named.qualifiedName());
    assertEquals(new PrimitiveType(Primitive.STRING), named.typeArguments().get(0));
    assertEquals(new ArrayType(new PrimitiveType(Primitive.I32), 1), named.typeArguments().get(1));
  }

  @Test
  void appliesSuffixesFromLeftToRight() throws IrSyntaxException {
    TypeRef type = parse("i32?[,]");
    ArrayType array = assertInstanceOf(ArrayType.class, type);
    assertEquals(2, array.rank());
    assertInstanceOf(NullableType.class, array.elementType());
  }

  @Test
  void equalTextsGiveEqualValues() throws IrSyntaxException {
    assertEquals(parse("List<i32>[]"), parse("List<i32>[]"));
    assertEquals(
        new NamedType("List", List.of(new PrimitiveType(Primitive.I32))), parse("List<i32>"));
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        "<>          | 10 | Typname erwartet",
        "List<>      | 15 | leere Typargumentliste",
        "i32[        | 13 | '[' nicht geschlossen",
        "i32[,       | 13 | '[' nicht geschlossen",
        "List<i32    | 14 | '<' nicht geschlossen",
        "List<i32;   | 18 | ',' oder '>' erwartet",
        "List<i32,>  | 19 | Typname erwartet",
        "i32<f32>    | 13 | keine Typargumente",
        "Foo.        | 14 | Bezeichner nach '.'",
        "1abc        | 10 | Typname erwartet",
        "i32&[]      | 14 | nach '&'",
        "i32??       | 14 | doppeltes '?'",
        "i32]        | 13 | unerwartetes ']'",
        "Foo::bar    | 13 | unerwartetes '::bar'"
      })
  void rejectsMalformedTypesWithPositionAndReason(String text, int column, String reason) {
    IrSyntaxException e = assertThrows(IrSyntaxException.class, () -> parse(text));
    assertEquals("t.ir:3:" + column, e.span().toString());
    assertTrue(e.reason().contains(reason), e.reason());
  }

  @Test
  void rejectsEmptyText() {
    IrSyntaxException e = assertThrows(IrSyntaxException.class, () -> parse(""));
    assertEquals("t.ir:3:10", e.span().toString());
  }

  @Test
  void rejectsArrayRankBelowOne() {
    assertThrows(
        IllegalArgumentException.class, () -> new ArrayType(new PrimitiveType(Primitive.I32), 0));
  }
}
