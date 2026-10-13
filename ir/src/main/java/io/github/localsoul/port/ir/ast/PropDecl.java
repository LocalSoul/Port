package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * {@code (prop ident type [:mods] [:get body] [:set body])}.
 *
 * @param name Name
 * @param type Typ
 * @param modifiers Modifier
 * @param getter Rumpf des Getters, oder {@code null}
 * @param setter Rumpf des Setters, Wert heißt {@code value}, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record PropDecl(
    String name,
    TypeRef type,
    List<Modifier> modifiers,
    Stmt getter,
    Stmt setter,
    NodeAttrs attrs,
    SourceSpan span)
    implements MemberDecl {

  /** Kopiert Listen in unveränderliche Listen. */
  public PropDecl {
    modifiers = List.copyOf(modifiers);
  }
}
