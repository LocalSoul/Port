package io.github.localsoul.port.ir.atom;

/**
 * Primitiver Typ, z. B. {@code i32}.
 *
 * @param primitive der primitive Typ
 */
public record PrimitiveType(Primitive primitive) implements TypeRef {

  @Override
  public String toString() {
    return primitive.spelling();
  }
}
