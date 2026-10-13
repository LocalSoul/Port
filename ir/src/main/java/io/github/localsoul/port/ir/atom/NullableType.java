package io.github.localsoul.port.ir.atom;

/**
 * {@code Nullable<T>}, geschrieben {@code T?}. Ob {@code T} ein Werttyp ist, prüft diese Ebene
 * nicht.
 *
 * @param underlyingType der zugrunde liegende Typ
 */
public record NullableType(TypeRef underlyingType) implements TypeRef {

  @Override
  public String toString() {
    return underlyingType + "?";
  }
}
