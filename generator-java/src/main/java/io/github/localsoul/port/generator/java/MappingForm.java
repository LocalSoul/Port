package io.github.localsoul.port.generator.java;

/** Dritte Spalte von {@code mapping.tsv}: wie ein IR-Symbol in Java erscheint. */
public enum MappingForm {
  /** Typ: Java-Symbol ist der voll qualifizierte Klassenname. */
  TYPE("type"),
  /** Instanzmethode: {@code ziel.name(args)}. */
  METHOD("method"),
  /** Statische Methode: {@code Typ.name(args)}. */
  STATIC_METHOD("static-method"),
  /** Instanz-Property oder -Feld lesend: {@code ziel.name()}. */
  GETTER("getter"),
  /** Statische Property lesend: {@code Typ.name()}. */
  STATIC_GETTER("static-getter"),
  /** Konstruktor: {@code new Typ(args)}. */
  CONSTRUCTOR("constructor"),
  /** Operator als statische Methode: {@code Typ.name(a, b)}. */
  OPERATOR("operator"),
  /**
   * Instanzmethode als statische Hilfe mit dem Empfänger als erstem Argument: {@code Typ.name(ziel,
   * args)} (z. B. {@code Strings.substring(s, 1)}).
   */
  HELPER("helper"),
  /** Lifecycle-Nachricht: gleich signierte Methoden in Unterklassen werden {@code @Override}. */
  MESSAGE("message");

  private final String spelling;

  MappingForm(String spelling) {
    this.spelling = spelling;
  }

  /**
   * Liefert die Schreibweise in {@code mapping.tsv}.
   *
   * @return die Schreibweise in {@code mapping.tsv}
   */
  public String spelling() {
    return spelling;
  }

  /**
   * Sucht die Form zu einer Schreibweise.
   *
   * @param text Schreibweise
   * @return Form oder {@code null}
   */
  public static MappingForm fromSpelling(String text) {
    for (MappingForm form : values()) {
      if (form.spelling.equals(text)) {
        return form;
      }
    }
    return null;
  }
}
