package io.github.localsoul.port.ir.printer;

import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EnumMember;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.SymbolIds;
import io.github.localsoul.port.ir.ast.TypeDecl;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Kanonische Reihenfolge (Spec Abschnitt 8): Typen nach ID, Mitglieder nach Gruppe (Felder,
 * Properties, Events, Konstruktoren, Methoden), innerhalb der Gruppe nach ID. Vergleich ordinal
 * ({@link String#compareTo}), unabhängig von der Locale. Die Sortierung ist stabil, gleiche IDs
 * behalten die Eingabereihenfolge (das ist ohnehin IR004).
 */
final class CanonicalOrder {

  private CanonicalOrder() {}

  static List<TypeDecl> types(List<TypeDecl> types) {
    List<TypeDecl> sorted = new ArrayList<>(types);
    sorted.sort(Comparator.comparing(SymbolIds::of));
    return sorted;
  }

  static List<MemberDecl> members(String typeName, List<MemberDecl> members) {
    List<MemberDecl> sorted = new ArrayList<>(members);
    sorted.sort(new MemberComparator(typeName));
    return sorted;
  }

  static List<EnumMember> enumMembers(List<EnumMember> members) {
    List<EnumMember> sorted = new ArrayList<>(members);
    sorted.sort(Comparator.comparing(EnumMember::name));
    return sorted;
  }

  /** Rang der Mitgliedsgruppe. Events stehen nach Properties (Spec nennt sie nicht). */
  static int groupRank(MemberDecl member) {
    return switch (member) {
      case FieldDecl f -> 0;
      case PropDecl p -> 1;
      case EventDecl e -> 2;
      case CtorDecl c -> 3;
      case MethodDecl m -> 4;
    };
  }

  private record MemberComparator(String typeName) implements Comparator<MemberDecl> {

    @Override
    public int compare(MemberDecl a, MemberDecl b) {
      int rankDiff = Integer.compare(groupRank(a), groupRank(b));
      if (rankDiff != 0) {
        return rankDiff;
      }
      String idA = SymbolIds.of(typeName, a);
      String idB = SymbolIds.of(typeName, b);
      return idA.compareTo(idB);
    }
  }
}
