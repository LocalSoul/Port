package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * {@code (field ident type [:mods] [:attrs] [:init expr])}.
 *
 * @param name Name
 * @param type Typ
 * @param modifiers Modifier
 * @param attributes Attribute
 * @param initializer Startwert, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record FieldDecl(
    String name,
    TypeRef type,
    List<Modifier> modifiers,
    List<AttributeUse> attributes,
    Expr initializer,
    NodeAttrs attrs,
    SourceSpan span)
    implements MemberDecl {

  /** Kopiert Listen in unveränderliche Listen. */
  public FieldDecl {
    modifiers = List.copyOf(modifiers);
    attributes = List.copyOf(attributes);
  }
}
