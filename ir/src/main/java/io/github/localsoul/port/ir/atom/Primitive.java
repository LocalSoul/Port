package io.github.localsoul.port.ir.atom;

/** Primitive Typen der IR ({@code docs/ir-spec.md} Abschnitt 3). */
public enum Primitive {
  BOOL("bool"),
  CHAR("char"),
  STRING("string"),
  OBJECT("object"),
  VOID("void"),
  I8("i8"),
  U8("u8"),
  I16("i16"),
  U16("u16"),
  I32("i32"),
  U32("u32"),
  I64("i64"),
  U64("u64"),
  F32("f32"),
  F64("f64"),
  /** Nativer 64-Bit-Zeiger (Spec Abschnitt 12). */
  PTR("ptr");

  private final String spelling;

  Primitive(String spelling) {
    this.spelling = spelling;
  }

  /** Liefert die Schreibweise im IR-Text, z. B. {@code "i32"}. */
  public String spelling() {
    return spelling;
  }

  /**
   * Sucht den primitiven Typ zu einer Schreibweise.
   *
   * @param text Schreibweise, z. B. {@code "string"}
   * @return der Typ oder {@code null}, wenn {@code text} kein primitiver Typ ist
   */
  public static Primitive fromSpelling(String text) {
    for (Primitive primitive : values()) {
      if (primitive.spelling.equals(text)) {
        return primitive;
      }
    }
    return null;
  }
}
