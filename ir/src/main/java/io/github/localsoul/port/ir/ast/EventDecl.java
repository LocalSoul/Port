package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * {@code (event ident type [:mods])}.
 *
 * @param name Name
 * @param type Delegate-Typ
 * @param modifiers Modifier
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record EventDecl(
    String name, TypeRef type, List<Modifier> modifiers, NodeAttrs attrs, SourceSpan span)
    implements MemberDecl {

  /** Kopiert Listen in unveränderliche Listen. */
  public EventDecl {
    modifiers = List.copyOf(modifiers);
  }
}
