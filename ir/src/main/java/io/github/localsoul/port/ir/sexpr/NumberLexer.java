package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;

/**
 * Prüft ein Zahl-Atom nach {@code docs/ir-spec.md} Abschnitt 2 und baut das Token.
 *
 * <p>Grammatik: {@code ["-"] ziffern ["." ziffern] [("e"|"E") ["+"|"-"] ziffern] suffix} oder
 * {@code "0x" hexziffern ganzzahlsuffix}.
 */
final class NumberLexer {

  private final String atom;
  private final SourceSpan span;
  private int pos;

  private NumberLexer(String atom, SourceSpan span) {
    this.atom = atom;
    this.span = span;
  }

  /**
   * Wandelt ein Atom, das mit einer Ziffer oder {@code -Ziffer} beginnt, in ein Token.
   *
   * @param atom vollständiges Atom
   * @param span Position des ersten Zeichens
   * @return {@link TokenKind#NUMBER} oder {@link TokenKind#BARE_INTEGER}
   * @throws IrSyntaxException wenn das Atom keine gültige Zahl ist
   */
  static Token lex(String atom, SourceSpan span) throws IrSyntaxException {
    NumberLexer lexer = new NumberLexer(atom, span);
    return lexer.lexNumber();
  }

  private Token lexNumber() throws IrSyntaxException {
    if (atom.startsWith("0x")) {
      return lexHex();
    }
    if (atom.startsWith("-0x")) {
      throw error("Hexzahlen haben kein Vorzeichen");
    }
    boolean negative = consume('-');
    readDigits("Ziffern");
    boolean fractional = false;
    if (consume('.')) {
      fractional = true;
      readDigits("Ziffern nach dem Dezimalpunkt");
    }
    boolean exponent = false;
    if (consume('e') || consume('E')) {
      exponent = true;
      if (!consume('+')) {
        consume('-');
      }
      readDigits("Ziffern im Exponenten");
    }
    String suffixText = atom.substring(pos);
    if (suffixText.isEmpty()) {
      if (negative || fractional || exponent) {
        throw error("Zahl ohne Typsuffix (z. B. 5.0f32, 0i32)");
      }
      return new Token(TokenKind.BARE_INTEGER, atom, atom, null, span);
    }
    NumberSuffix suffix = parseSuffix(suffixText);
    if ((fractional || exponent) && !suffix.isFloatingPoint()) {
      throw error("Ganzzahl-Suffix '" + suffixText + "' an Gleitkommazahl");
    }
    return new Token(TokenKind.NUMBER, atom, atom, suffix, span);
  }

  private Token lexHex() throws IrSyntaxException {
    pos = 2;
    int digitsStart = pos;
    while (pos < atom.length() && Character.digit(atom.charAt(pos), 16) >= 0) {
      pos++;
    }
    if (pos == digitsStart) {
      throw error("Hexzahl ohne Ziffern");
    }
    String suffixText = atom.substring(pos);
    if (suffixText.isEmpty()) {
      throw error("Zahl ohne Typsuffix (z. B. 0xFFu32)");
    }
    NumberSuffix suffix = parseSuffix(suffixText);
    if (suffix.isFloatingPoint()) {
      throw error("Hexzahlen erlauben nur Ganzzahl-Suffixe");
    }
    return new Token(TokenKind.NUMBER, atom, atom, suffix, span);
  }

  private NumberSuffix parseSuffix(String suffixText) throws IrSyntaxException {
    NumberSuffix suffix = NumberSuffix.fromSpelling(suffixText);
    if (suffix == null) {
      throw error("ungültiges Typsuffix '" + suffixText + "'");
    }
    return suffix;
  }

  private void readDigits(String what) throws IrSyntaxException {
    int start = pos;
    while (pos < atom.length() && IrLexer.isAsciiDigit(atom.charAt(pos))) {
      pos++;
    }
    if (pos == start) {
      throw error("erwartet " + what);
    }
  }

  private boolean consume(char expected) {
    if (pos < atom.length() && atom.charAt(pos) == expected) {
      pos++;
      return true;
    }
    return false;
  }

  private IrSyntaxException error(String reason) {
    return new IrSyntaxException(span, reason + " in '" + atom + "'");
  }
}
