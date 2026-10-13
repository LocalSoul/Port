package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.InterfaceDecl;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.StructDecl;
import io.github.localsoul.port.ir.ast.TypeAttrs;
import io.github.localsoul.port.ir.ast.TypeDecl;
import java.util.List;

/** Einheitlicher Zugriff auf Mitglieder und Typattribute aller Typdeklarationen. */
final class TypeMembers {

  private TypeMembers() {}

  /** Mitglieder; leer bei Enums (deren Werte sind {@code EnumMember}). */
  static List<MemberDecl> of(TypeDecl type) {
    return switch (type) {
      case ClassDecl c -> c.members();
      case StructDecl s -> s.members();
      case InterfaceDecl i -> i.members();
      case EnumDecl e -> List.of();
    };
  }

  /** Typattribute; {@code null} bei Enums. */
  static TypeAttrs attrs(TypeDecl type) {
    return switch (type) {
      case ClassDecl c -> c.typeAttrs();
      case StructDecl s -> s.typeAttrs();
      case InterfaceDecl i -> i.typeAttrs();
      case EnumDecl e -> null;
    };
  }
}
