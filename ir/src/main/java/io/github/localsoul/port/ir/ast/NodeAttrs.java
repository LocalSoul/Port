package io.github.localsoul.port.ir.ast;

/**
 * Attribute, die jeder Knoten am Ende tragen darf (Spec Abschnitt 8).
 *
 * @param loc Herkunft, z. B. {@code "Datei.cs:12"}, oder {@code null}
 * @param manual Grund für Handarbeit, oder {@code null}
 * @param note Freitext, oder {@code null}
 */
public record NodeAttrs(String loc, String manual, String note) {

  /** Knoten ohne Attribute. */
  public static final NodeAttrs NONE = new NodeAttrs(null, null, null);

  /**
   * Liefert {@code true}, wenn kein Attribut gesetzt ist.
   *
   * @return {@code true}, wenn kein Attribut gesetzt ist
   */
  public boolean isEmpty() {
    return loc == null && manual == null && note == null;
  }
}
