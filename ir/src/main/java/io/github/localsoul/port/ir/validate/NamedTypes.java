package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.atom.ArrayType;
import io.github.localsoul.port.ir.atom.ByRefType;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.NullableType;
import io.github.localsoul.port.ir.atom.PrimitiveType;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.ArrayList;
import java.util.List;

/** Zerlegt Typreferenzen in die benannten Typen, die sie enthalten. */
final class NamedTypes {

  /** Platzhalter des Frontends für nicht aufgelöste Typen (IR003). */
  static final String UNKNOWN = "unknown";

  private NamedTypes() {}

  /**
   * Liefert alle benannten Typen in {@code type}, äußere vor inneren, von links nach rechts.
   * Arrays, Nullable und ByRef werden ausgepackt, Primitive entfallen.
   */
  static List<NamedType> in(TypeRef type) {
    List<NamedType> result = new ArrayList<>();
    collect(type, result);
    return result;
  }

  private static void collect(TypeRef type, List<NamedType> into) {
    switch (type) {
      case PrimitiveType p -> {
        // keine benannten Typen
      }
      case ArrayType a -> collect(a.elementType(), into);
      case NullableType n -> collect(n.underlyingType(), into);
      case ByRefType b -> collect(b.referencedType(), into);
      case NamedType n -> {
        into.add(n);
        for (TypeRef argument : n.typeArguments()) {
          collect(argument, into);
        }
      }
    }
  }

  /** Name ohne Typargumente; bei Nicht-Namen (z. B. Arrays) {@code null}. */
  static String baseName(TypeRef type) {
    if (type instanceof NamedType named) {
      return named.qualifiedName();
    }
    return null;
  }
}
