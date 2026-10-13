package io.github.localsoul.port.ir.atom;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;

/**
 * Typreferenz nach {@code docs/ir-spec.md} Abschnitt 3.
 *
 * <p>Suffixe werden von links nach rechts angewendet: {@code i32?[]} ist ein Array von {@code
 * i32?}. {@code toString()} liefert die kanonische Schreibweise ohne Leerzeichen.
 */
public sealed interface TypeRef
    permits PrimitiveType, NamedType, ArrayType, NullableType, ByRefType {

  /**
   * Parst eine Typreferenz.
   *
   * @param text Typ-Atom, z. B. {@code Dictionary<string,i32[]>}
   * @param atomStart Position des ersten Zeichens von {@code text}; Fehlerpositionen werden relativ
   *     dazu berechnet
   * @return die Typreferenz
   * @throws IrSyntaxException wenn {@code text} kein gültiger Typ ist; Position zeigt auf die
   *     Fehlerstelle im Atom
   */
  static TypeRef parse(String text, SourceSpan atomStart) throws IrSyntaxException {
    AtomParser parser = new AtomParser(text, atomStart);
    return parser.parseWholeType();
  }
}
