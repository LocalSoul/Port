package io.github.localsoul.port.generator.java;

import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.SymbolIds;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Feste Reihenfolge der Member in der Ausgabe wie im kanonischen IR-Printer: Felder, Properties,
 * Events, Konstruktoren, Methoden, je Gruppe nach stabiler ID. Damit ist die Ausgabe unabhängig von
 * der Reihenfolge in der Eingabe.
 */
final class MemberOrder {

  private MemberOrder() {}

  static List<MemberDecl> sorted(String typeName, List<MemberDecl> members) {
    List<MemberDecl> result = new ArrayList<>(members);
    result.sort(
        Comparator.comparingInt(MemberOrder::rank)
            .thenComparing(member -> SymbolIds.of(typeName, member)));
    return result;
  }

  private static int rank(MemberDecl member) {
    return switch (member) {
      case FieldDecl f -> 0;
      case PropDecl p -> 1;
      case EventDecl e -> 2;
      case CtorDecl c -> 3;
      case MethodDecl m -> 4;
    };
  }
}
