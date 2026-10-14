package io.github.localsoul.port.ir.refs;

import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.EnumMember;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.InterfaceDecl;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.StructDecl;
import io.github.localsoul.port.ir.ast.SymbolIds;
import io.github.localsoul.port.ir.ast.TypeDecl;
import java.util.ArrayList;
import java.util.List;

/** Sammelt alle Deklarationen einer IR-Datei in Eingabereihenfolge (Typ, dann seine Mitglieder). */
public final class Declarations {

  private Declarations() {}

  /**
   * Sammelt die Deklarationen einer Datei.
   *
   * @param file gelesene IR-Datei
   * @return Typen und Mitglieder in Eingabereihenfolge
   */
  public static List<Declaration> collect(IrFile file) {
    List<Declaration> result = new ArrayList<>();
    for (TypeDecl type : file.types()) {
      String name = type.name();
      result.add(
          new Declaration(SymbolIds.of(type), typeKind(type), simpleName(name), name, type.span()));
      switch (type) {
        case ClassDecl c -> members(name, c.members(), result);
        case StructDecl s -> members(name, s.members(), result);
        case InterfaceDecl i -> members(name, i.members(), result);
        case EnumDecl e -> {
          for (EnumMember member : e.members()) {
            result.add(
                new Declaration(
                    SymbolIds.of(name, member), "enum-member", member.name(), name, member.span()));
          }
        }
      }
    }
    return result;
  }

  private static void members(String typeName, List<MemberDecl> members, List<Declaration> result) {
    for (MemberDecl member : members) {
      String symbol = SymbolIds.of(typeName, member);
      result.add(
          new Declaration(symbol, memberKind(member), memberName(symbol), typeName, member.span()));
    }
  }

  private static String typeKind(TypeDecl type) {
    return switch (type) {
      case ClassDecl c -> "class";
      case StructDecl s -> "struct";
      case InterfaceDecl i -> "interface";
      case EnumDecl e -> "enum";
    };
  }

  private static String memberKind(MemberDecl member) {
    return switch (member) {
      case FieldDecl f -> "field";
      case PropDecl p -> "prop";
      case EventDecl e -> "event";
      case CtorDecl c -> "ctor";
      case MethodDecl m -> "method";
    };
  }

  /** Teil nach {@code ::} bis vor {@code #} bzw. {@code <}. */
  private static String memberName(String symbol) {
    String name = symbol.substring(symbol.indexOf("::") + 2);
    int end = name.length();
    int hash = name.indexOf('#');
    if (hash >= 0) {
      end = hash;
    }
    int generic = name.indexOf('<');
    if (generic >= 0 && generic < end) {
      end = generic;
    }
    return name.substring(0, end);
  }

  private static String simpleName(String qualified) {
    return qualified.substring(qualified.lastIndexOf('.') + 1);
  }
}
