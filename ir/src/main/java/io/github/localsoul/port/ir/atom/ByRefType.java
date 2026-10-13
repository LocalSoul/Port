package io.github.localsoul.port.ir.atom;

/**
 * {@code ref}/{@code out}/{@code in}-Typ, geschrieben {@code T&}. Steht immer außen, also als
 * letztes Suffix.
 *
 * @param referencedType der referenzierte Typ
 */
public record ByRefType(TypeRef referencedType) implements TypeRef {

  @Override
  public String toString() {
    return referencedType + "&";
  }
}
