package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * Typattribute von {@code class}, {@code struct} und {@code interface} (Spec Abschnitt 5).
 *
 * @param baseType {@code :base}, oder {@code null}
 * @param implementedTypes {@code :implements}
 * @param generics {@code :generics}, Namen der Typparameter
 * @param modifiers {@code :mods}
 * @param attributes {@code :attrs}
 */
public record TypeAttrs(
    TypeRef baseType,
    List<TypeRef> implementedTypes,
    List<String> generics,
    List<Modifier> modifiers,
    List<AttributeUse> attributes) {

  /** Kopiert Listen in unveränderliche Listen. */
  public TypeAttrs {
    implementedTypes = List.copyOf(implementedTypes);
    generics = List.copyOf(generics);
    modifiers = List.copyOf(modifiers);
    attributes = List.copyOf(attributes);
  }
}
