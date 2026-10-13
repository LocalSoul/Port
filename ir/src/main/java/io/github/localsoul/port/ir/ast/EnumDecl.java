package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * {@code (enum qname :underlying type {(ident number)})}.
 *
 * @param name qualifizierter Name
 * @param underlyingType zugrunde liegender Typ
 * @param members Werte in Eingabereihenfolge
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record EnumDecl(
    String name, TypeRef underlyingType, List<EnumMember> members, NodeAttrs attrs, SourceSpan span)
    implements TypeDecl {

  /** Kopiert Listen in unveränderliche Listen. */
  public EnumDecl {
    members = List.copyOf(members);
  }
}
