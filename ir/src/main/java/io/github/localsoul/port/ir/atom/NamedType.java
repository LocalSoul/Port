package io.github.localsoul.port.ir.atom;

import java.util.List;

/**
 * Benannter Typ mit optionalen Typargumenten, z. B. {@code System.Collections.Generic.List<i32>}
 * oder ein Typparameter {@code T}.
 *
 * @param qualifiedName voll qualifizierter Name ({@code ident { "." ident }})
 * @param typeArguments Typargumente, leer bei nicht-generischen Typen (unveränderlich)
 */
public record NamedType(String qualifiedName, List<TypeRef> typeArguments) implements TypeRef {

  /** Kopiert die Typargumente in eine unveränderliche Liste. */
  public NamedType {
    typeArguments = List.copyOf(typeArguments);
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder(qualifiedName);
    AtomParser.appendTypeList(out, '<', typeArguments, '>');
    return out.toString();
  }
}
