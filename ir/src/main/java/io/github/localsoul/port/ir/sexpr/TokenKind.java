package io.github.localsoul.port.ir.sexpr;

/** Art eines Tokens des {@link IrLexer}. */
public enum TokenKind {
  /** {@code (} */
  LPAREN,
  /** {@code )} */
  RPAREN,
  /** String-Literal, {@link Token#value()} enthält den Text nach Auflösung der Escapes. */
  STRING,
  /** Zahl mit Typsuffix (dezimal oder hexadezimal). */
  NUMBER,
  /**
   * Nicht-negative Ganzzahl ohne Suffix. Nur an wenigen Stellen erlaubt ({@code (char 97)}, {@code
   * :version 1}, {@code :size 24}), die Prüfung übernimmt die Ebene darüber.
   */
  BARE_INTEGER,
  /** Keyword wie {@code :base}, {@link Token#value()} enthält den Namen ohne Doppelpunkt. */
  KEYWORD,
  /** Jedes andere Atom: Bezeichner, Typen, Member-Referenzen, Operatoren, {@code true}, ... */
  SYMBOL,
  /** Ende der Eingabe. */
  EOF
}
