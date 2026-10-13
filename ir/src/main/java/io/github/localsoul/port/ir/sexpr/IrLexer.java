package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.Identifiers;
import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;

/**
 * Zerlegt IR-Text in Tokens nach {@code docs/ir-spec.md} Abschnitt 2.
 *
 * <p>Regeln dieser Implementierung:
 *
 * <ul>
 *   <li>Trennzeichen sind Leerzeichen, Tabulator, {@code \r}, {@code \n}, Klammern, {@code "} und
 *       {@code ;}. Nur {@code \n} beginnt eine neue Zeile.
 *   <li>Ein Atom, das mit einer Ziffer oder mit {@code -} gefolgt von einer Ziffer beginnt, ist
 *       eine Zahl. Sie braucht ein Typsuffix. Einzige Ausnahme ist eine nicht-negative Ganzzahl
 *       ohne Punkt und Exponent, sie wird als {@link TokenKind#BARE_INTEGER} geliefert.
 *   <li>Exponent: {@code e} oder {@code E}, optional {@code +}/{@code -}, dann Ziffern. Zahlen mit
 *       Punkt oder Exponent brauchen ein Gleitkomma-Suffix ({@code f32}, {@code f64}).
 *   <li>Hexzahlen ({@code 0x...}) haben kein Vorzeichen und nur Ganzzahl-Suffixe.
 *   <li>Strings dürfen Zeilenumbrüche enthalten. Erlaubte Escapes: {@code \\ \" \n \r \t \}{@code
 *       uXXXX}.
 * </ul>
 *
 * <p>Instanzen sind nicht threadsicher und werden für genau eine Eingabe benutzt.
 */
public final class IrLexer {

  private final String text;
  private final String fileName;
  private int pos;
  private int line = 1;
  private int column = 1;

  /**
   * Erzeugt einen Lexer.
   *
   * @param text IR-Text
   * @param fileName Dateiname für Positionsangaben
   */
  public IrLexer(String text, String fileName) {
    this.text = text;
    this.fileName = fileName;
  }

  /**
   * Liefert das nächste Token. Am Ende der Eingabe wird wiederholt {@link TokenKind#EOF} geliefert.
   *
   * @return das nächste Token
   * @throws IrSyntaxException bei ungültigen Zahlen, Keywords, Escapes oder nicht geschlossenen
   *     Strings
   */
  public Token next() throws IrSyntaxException {
    skipWhitespaceAndComments();
    SourceSpan start = currentSpan();
    if (pos >= text.length()) {
      return new Token(TokenKind.EOF, "", "", null, start);
    }
    char c = text.charAt(pos);
    if (c == '(') {
      advance();
      return new Token(TokenKind.LPAREN, "(", "(", null, start);
    }
    if (c == ')') {
      advance();
      return new Token(TokenKind.RPAREN, ")", ")", null, start);
    }
    if (c == '"') {
      return readString(start);
    }
    String atom = readAtom();
    if (startsNumber(atom)) {
      return NumberLexer.lex(atom, start);
    }
    if (atom.charAt(0) == ':') {
      return keywordToken(atom, start);
    }
    return new Token(TokenKind.SYMBOL, atom, atom, null, start);
  }

  private void skipWhitespaceAndComments() {
    while (pos < text.length()) {
      char c = text.charAt(pos);
      if (c == ';') {
        while (pos < text.length() && text.charAt(pos) != '\n') {
          advance();
        }
      } else if (isWhitespace(c)) {
        advance();
      } else {
        return;
      }
    }
  }

  private String readAtom() {
    int begin = pos;
    while (pos < text.length() && !isDelimiter(text.charAt(pos))) {
      advance();
    }
    return text.substring(begin, pos);
  }

  private Token readString(SourceSpan start) throws IrSyntaxException {
    int begin = pos;
    advance();
    StringBuilder value = new StringBuilder();
    while (true) {
      if (pos >= text.length()) {
        throw new IrSyntaxException(start, "String nicht geschlossen");
      }
      char c = text.charAt(pos);
      if (c == '"') {
        advance();
        String raw = text.substring(begin, pos);
        return new Token(TokenKind.STRING, raw, value.toString(), null, start);
      }
      if (c == '\\') {
        value.append(readEscape(start));
      } else {
        value.append(c);
        advance();
      }
    }
  }

  private char readEscape(SourceSpan stringStart) throws IrSyntaxException {
    SourceSpan escapeStart = currentSpan();
    advance();
    if (pos >= text.length()) {
      throw new IrSyntaxException(stringStart, "String nicht geschlossen");
    }
    char c = text.charAt(pos);
    advance();
    switch (c) {
      case '\\':
        return '\\';
      case '"':
        return '"';
      case 'n':
        return '\n';
      case 'r':
        return '\r';
      case 't':
        return '\t';
      case 'u':
        return readUnicodeEscape(escapeStart);
      default:
        throw new IrSyntaxException(escapeStart, "ungültiges Escape '\\" + c + "' im String");
    }
  }

  private char readUnicodeEscape(SourceSpan escapeStart) throws IrSyntaxException {
    int value = 0;
    for (int i = 0; i < 4; i++) {
      int digit = -1;
      if (pos < text.length()) {
        digit = Character.digit(text.charAt(pos), 16);
      }
      if (digit < 0) {
        throw new IrSyntaxException(
            escapeStart, "ungültiges Escape: \\u braucht genau 4 Hexziffern");
      }
      value = value * 16 + digit;
      advance();
    }
    return (char) value;
  }

  private Token keywordToken(String atom, SourceSpan start) throws IrSyntaxException {
    String name = atom.substring(1);
    if (!isKeywordName(name)) {
      throw new IrSyntaxException(start, "ungültiges Keyword '" + atom + "'");
    }
    return new Token(TokenKind.KEYWORD, atom, name, null, start);
  }

  /** Prüft {@code ident { "-" ident }}. */
  private static boolean isKeywordName(String name) {
    String[] parts = name.split("-", -1);
    for (String part : parts) {
      if (!Identifiers.isIdent(part)) {
        return false;
      }
    }
    return true;
  }

  private static boolean startsNumber(String atom) {
    char first = atom.charAt(0);
    if (isAsciiDigit(first)) {
      return true;
    }
    return first == '-' && atom.length() > 1 && isAsciiDigit(atom.charAt(1));
  }

  static boolean isAsciiDigit(char c) {
    return c >= '0' && c <= '9';
  }

  private static boolean isWhitespace(char c) {
    return c == ' ' || c == '\t' || c == '\n' || c == '\r';
  }

  private static boolean isDelimiter(char c) {
    return isWhitespace(c) || c == '(' || c == ')' || c == '"' || c == ';';
  }

  private SourceSpan currentSpan() {
    return new SourceSpan(fileName, line, column);
  }

  /** Geht ein UTF-16-Zeichen weiter. Die Spalte zählt Codepoints, nicht UTF-16-Einheiten. */
  private void advance() {
    char c = text.charAt(pos);
    pos++;
    if (c == '\n') {
      line++;
      column = 1;
    } else if (!Character.isLowSurrogate(c)) {
      column++;
    }
  }
}
