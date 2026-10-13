package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * {@code (interface qname typeattrs {member})}.
 *
 * @param name qualifizierter Name
 * @param typeAttrs Typattribute
 * @param members Mitglieder in Eingabereihenfolge
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record InterfaceDecl(
    String name, TypeAttrs typeAttrs, List<MemberDecl> members, NodeAttrs attrs, SourceSpan span)
    implements TypeDecl {

  /** Kopiert Listen in unveränderliche Listen. */
  public InterfaceDecl {
    members = List.copyOf(members);
  }
}
