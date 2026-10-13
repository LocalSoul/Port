package io.github.localsoul.port.ir.reader;

import java.util.Set;

/**
 * Bekannte Keywords einer Knotenart. {@code :loc}, {@code :manual} und {@code :note} sind immer
 * bekannt und müssen nicht angegeben werden.
 *
 * @param valueKeywords Keywords mit genau einem Wert
 * @param flagKeywords Keywords ohne Wert, z. B. {@code :abstract}
 */
record KeywordSpec(Set<String> valueKeywords, Set<String> flagKeywords) {

  /** Nur die allgemeinen Attribute. */
  static final KeywordSpec NONE = new KeywordSpec(Set.of(), Set.of());

  /** Kopiert die Mengen. */
  KeywordSpec {
    valueKeywords = Set.copyOf(valueKeywords);
    flagKeywords = Set.copyOf(flagKeywords);
  }

  /** Keywords mit Wert, keine Flags. */
  static KeywordSpec values(String... names) {
    return new KeywordSpec(Set.of(names), Set.of());
  }
}
