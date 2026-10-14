package io.github.localsoul.port.generator.java;

/** Dritte Spalte von {@code mapping.tsv}: wie ein IR-Symbol in Java erscheint. */
public enum MappingForm {
  /** Typ: Java-Symbol ist der voll qualifizierte Klassenname. */
  TYPE("type"),
  /**
   * Werttyp (struct) als unveränderlicher Record (ADR-009): wie {@link #TYPE}, zusätzlich mit
   * {@code withFeld(...)} für Feldänderungen und der Konstante {@code DEFAULT}.
   */
  STRUCT("struct"),
  /** Instanzmethode: {@code ziel.name(args)}. */
  METHOD("method"),
  /** Statische Methode: {@code Typ.name(args)}. */
  STATIC_METHOD("static-method"),
  /** Instanz-Property oder -Feld lesend: {@code ziel.name()}. */
  GETTER("getter"),
  /** Instanz-Property schreibend, Schlüssel {@code Typ::name:set}: {@code ziel.name(wert)}. */
  SETTER("setter"),
  /** Statische Property schreibend, Schlüssel {@code Typ::name:set}: {@code Typ.name(wert)}. */
  STATIC_SETTER("static-setter"),
  /** Statische Konstante oder Enum-Wert ohne Aufruf: {@code Typ.NAME}. */
  CONSTANT("constant"),
  /**
   * Generische Methode mit einem Typargument, das zur Instanzierung dient: {@code
   * ziel.name(Arg::new)} (z. B. {@code AddComponent<T>}), ohne Reflection.
   */
  FACTORY("factory"),
  /** Mutierende Methode eines Werttyps (ADR-009): als Statement {@code ziel = ziel.name(args)}. */
  MUTATOR("mutator"),
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
