package io.github.localsoul.port.ir;

/** Prüft Bezeichner und qualifizierte Namen nach {@code docs/ir-spec.md} Abschnitt 2. */
public final class Identifiers {

  private Identifiers() {}

  /**
   * Prüft {@code ident := [A-Za-z_][A-Za-z0-9_]*}.
   *
   * @param s zu prüfender Text, nicht {@code null}
   * @return {@code true}, wenn {@code s} ein Bezeichner ist
   */
  public static boolean isIdent(String s) {
    if (s.isEmpty()) {
      return false;
    }
    for (int i = 0; i < s.length(); i++) {
      char c = s.charAt(i);
      boolean letter = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
      boolean digit = c >= '0' && c <= '9';
      if (!letter && !(digit && i > 0)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Prüft {@code qname := ident { "." ident }}.
   *
   * @param s zu prüfender Text, nicht {@code null}
   * @return {@code true}, wenn {@code s} ein qualifizierter Name ist
   */
  public static boolean isQualifiedName(String s) {
    String[] parts = s.split("\\.", -1);
    for (String part : parts) {
      if (!isIdent(part)) {
        return false;
      }
    }
    return true;
  }
}
