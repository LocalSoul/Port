package io.github.localsoul.port.ir.sexpr;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

class IrLexerTest {

  private static List<Token> lex(String text) throws IrSyntaxException {
    IrLexer lexer = new IrLexer(text, "t.ir");
    List<Token> tokens = new ArrayList<>();
    Token token = lexer.next();
    while (token.kind() != TokenKind.EOF) {
      tokens.add(token);
      token = lexer.next();
    }
    return tokens;
  }

  private static Token single(String text) throws IrSyntaxException {
    List<Token> tokens = lex(text);
    assertEquals(1, tokens.size(), "Anzahl Tokens für " + text);
    return tokens.get(0);
  }

  private static IrSyntaxException rejects(String text) {
    return assertThrows(IrSyntaxException.class, () -> lex(text));
  }

  @Test
  void lexesParenthesesAndAtoms() throws IrSyntaxException {
    List<Token> tokens = lex("(class Mover :base UnityEngine.MonoBehaviour)");
    assertEquals(6, tokens.size());
    assertEquals(TokenKind.LPAREN, tokens.get(0).kind());
    assertEquals(TokenKind.SYMBOL, tokens.get(1).kind());
    assertEquals("class", tokens.get(1).text());
    assertEquals("Mover", tokens.get(2).text());
    assertEquals(TokenKind.KEYWORD, tokens.get(3).kind());
    assertEquals("base", tokens.get(3).value());
    assertEquals(":base", tokens.get(3).text());
    assertEquals("UnityEngine.MonoBehaviour", tokens.get(4).text());
    assertEquals(TokenKind.RPAREN, tokens.get(5).kind());
    assertEquals("t.ir:1:45", tokens.get(5).span().toString());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "Dictionary<string,i32[]>",
        "f32[,]",
        "UnityEngine.Transform::Translate#UnityEngine.Vector3",
        "Foo::.ctor#f32,f32,f32",
        "Foo::Bar#",
        "<=",
        "<<",
        "-",
        "&&",
        "true",
        "null",
        "i32?",
        "T&"
      })
  void keepsTypeAndMemberAtomsWhole(String atom) throws IrSyntaxException {
    Token token = single(atom);
    assertEquals(TokenKind.SYMBOL, token.kind());
    assertEquals(atom, token.text());
  }

  @ParameterizedTest
  @CsvSource({
    "5.0f32, F32",
    "0i32, I32",
    "7u64, U64",
    "-12i64, I64",
    "1e5f64, F64",
    "1.5E-3f32, F32",
    "2.0e+10f64, F64",
    "5f32, F32",
    "0x7FFE0000u64, U64",
    "0xffi8, I8",
    "255u8, U8",
    "-3i16, I16",
    "3u16, U16",
    "3u32, U32"
  })
  void acceptsNumbersWithSuffix(String text, NumberSuffix expected) throws IrSyntaxException {
    Token token = single(text);
    assertEquals(TokenKind.NUMBER, token.kind());
    assertEquals(expected, token.suffix());
    assertEquals(text, token.text());
  }

  @Test
  void rejectsDecimalWithoutSuffixWithPosition() {
    IrSyntaxException e = rejects("(field speed f32\n  :init 5.0)");
    assertEquals("t.ir:2:9", e.span().toString());
    assertTrue(e.reason().contains("ohne Typsuffix"), e.reason());
    assertTrue(e.getMessage().startsWith("t.ir:2:9: "), e.getMessage());
  }

  @ParameterizedTest
  @ValueSource(strings = {"5.0", "-5", "1e5", "0xFF", "-1.5"})
  void rejectsNumbersWithoutSuffix(String text) {
    IrSyntaxException e = rejects(text);
    assertTrue(e.reason().contains("ohne Typsuffix"), e.reason());
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"5.0i32", "1e3u8", "0x10f32", "5x32", "5.f32", "5.0e", "-0x1i32", "0xi32"})
  void rejectsMalformedNumbers(String text) {
    IrSyntaxException e = rejects(text);
    assertEquals("t.ir:1:1", e.span().toString());
  }

  @Test
  void returnsBareIntegerForUnsuffixedNaturalNumber() throws IrSyntaxException {
    Token token = single("97");
    assertEquals(TokenKind.BARE_INTEGER, token.kind());
    assertNull(token.suffix());
  }

  @Test
  void resolvesStringEscapes() throws IrSyntaxException {
    Token token = single("\"a\\\\b\\\"c\\nd\\re\\tf\\u00e4\\u20AC\"");
    assertEquals(TokenKind.STRING, token.kind());
    assertEquals("a\\b\"c\nd\re\tf\u00e4\u20ac", token.value());
  }

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {"\"ab\\x\" | 1:4", "\"\\u12\" | 1:2", "\"x\\u12G4\" | 1:3", "\"\\'\" | 1:2"})
  void rejectsInvalidEscapesAtEscapePosition(String text, String position) {
    IrSyntaxException e = rejects(text);
    assertEquals("t.ir:" + position, e.span().toString());
    assertTrue(e.reason().contains("Escape"), e.reason());
  }

  @Test
  void unclosedStringReportsOpeningQuote() {
    IrSyntaxException e = rejects("(raw cs\n  \"if (x) {\n  y();\n");
    assertEquals("t.ir:2:3", e.span().toString());
    assertTrue(e.reason().contains("String nicht geschlossen"), e.reason());
  }

  @Test
  void unclosedStringEndingInBackslashReportsOpeningQuote() {
    IrSyntaxException e = rejects("  \"abc\\");
    assertEquals("t.ir:1:3", e.span().toString());
  }

  @Test
  void stringsMayContainNewlinesAndPositionsContinue() throws IrSyntaxException {
    List<Token> tokens = lex("\"a\nb\" x");
    assertEquals("a\nb", tokens.get(0).value());
    assertEquals("t.ir:2:4", tokens.get(1).span().toString());
  }

  @Test
  void ignoresCommentsAndKeepsLinePositions() throws IrSyntaxException {
    String text = "; Kopf\n(a ; Kommentar ( mit \" Zeichen\n  b) ; Ende\n;;\n   c";
    List<Token> tokens = lex(text);
    assertEquals(5, tokens.size());
    assertEquals("t.ir:2:1", tokens.get(0).span().toString());
    assertEquals("t.ir:2:2", tokens.get(1).span().toString());
    assertEquals("b", tokens.get(2).text());
    assertEquals("t.ir:3:3", tokens.get(2).span().toString());
    assertEquals("t.ir:3:4", tokens.get(3).span().toString());
    assertEquals("c", tokens.get(4).text());
    assertEquals("t.ir:5:4", tokens.get(4).span().toString());
  }

  @Test
  void commentEndsAtomWithoutWhitespace() throws IrSyntaxException {
    List<Token> tokens = lex("abc;kommentar\ndef");
    assertEquals(2, tokens.size());
    assertEquals("abc", tokens.get(0).text());
    assertEquals("t.ir:2:1", tokens.get(1).span().toString());
  }

  @Test
  void countsColumnsInCodePoints() throws IrSyntaxException {
    List<Token> tokens = lex("\"\uD83D\uDE00\" x");
    assertEquals("t.ir:1:5", tokens.get(1).span().toString());
  }

  @Test
  void carriageReturnIsWhitespaceButNotNewline() throws IrSyntaxException {
    List<Token> tokens = lex("a\r\nb");
    assertEquals("t.ir:2:1", tokens.get(1).span().toString());
  }

  @ParameterizedTest
  @ValueSource(strings = {"base", "base-call", "loc", "a_1-b2"})
  void acceptsKeywords(String name) throws IrSyntaxException {
    Token token = single(":" + name);
    assertEquals(TokenKind.KEYWORD, token.kind());
    assertEquals(name, token.value());
  }

  @ParameterizedTest
  @ValueSource(strings = {":", ":1abc", ":base-", "::x", ":a.b", ":-a"})
  void rejectsMalformedKeywords(String text) {
    IrSyntaxException e = rejects("  " + text);
    assertEquals("t.ir:1:3", e.span().toString());
    assertTrue(e.reason().contains("Keyword"), e.reason());
  }

  @Test
  void returnsEofRepeatedly() throws IrSyntaxException {
    IrLexer lexer = new IrLexer("  ; nur Kommentar", "t.ir");
    assertEquals(TokenKind.EOF, lexer.next().kind());
    assertEquals(TokenKind.EOF, lexer.next().kind());
  }
}
