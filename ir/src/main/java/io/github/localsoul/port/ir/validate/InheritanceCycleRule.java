package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.TypeAttrs;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IR005: Vererbungszyklus über {@code :base} und {@code :implements} zwischen Typen der Datei.
 * Jeder Typ, der auf einem Zyklus liegt, wird gemeldet; die Meldung nennt den Pfad, z. B. {@code A
 * -> B -> A}. Kanten werden in Deklarationsreihenfolge verfolgt ({@code :base} zuerst), damit der
 * gemeldete Pfad deterministisch ist.
 */
final class InheritanceCycleRule {

  private final Map<String, TypeDecl> declaredTypes;

  private InheritanceCycleRule(Map<String, TypeDecl> declaredTypes) {
    this.declaredTypes = declaredTypes;
  }

  static void check(IrFile file, Map<String, TypeDecl> declaredTypes, DiagnosticSink sink) {
    InheritanceCycleRule rule = new InheritanceCycleRule(declaredTypes);
    for (TypeDecl type : file.types()) {
      List<String> cycle = rule.findCycle(type.name());
      if (cycle != null) {
        sink.add(
            "IR005",
            Severity.ERROR,
            type.name(),
            "Vererbungszyklus: " + String.join(" -> ", cycle),
            type.span(),
            null);
      }
    }
  }

  /** Pfad vom Typ zurück zu sich selbst, oder {@code null}. */
  private List<String> findCycle(String start) {
    List<String> path = new ArrayList<>();
    path.add(start);
    Set<String> visited = new HashSet<>();
    if (search(start, start, path, visited)) {
      return path;
    }
    return null;
  }

  /**
   * Tiefensuche; {@code path} enthält bei Erfolg den Zyklus inklusive Rückkehr zu {@code start}.
   */
  private boolean search(String current, String start, List<String> path, Set<String> visited) {
    for (String next : supertypes(current)) {
      if (next.equals(start)) {
        path.add(next);
        return true;
      }
      if (!visited.add(next)) {
        continue;
      }
      path.add(next);
      if (search(next, start, path, visited)) {
        return true;
      }
      path.remove(path.size() - 1);
    }
    return false;
  }

  /** Direkte Obertypen, die in der Datei deklariert sind. */
  private List<String> supertypes(String typeName) {
    List<String> result = new ArrayList<>();
    TypeDecl type = declaredTypes.get(typeName);
    if (type == null) {
      return result;
    }
    TypeAttrs attrs = TypeMembers.attrs(type);
    if (attrs == null) {
      return result;
    }
    addIfDeclared(attrs.baseType(), result);
    for (TypeRef implemented : attrs.implementedTypes()) {
      addIfDeclared(implemented, result);
    }
    return result;
  }

  private void addIfDeclared(TypeRef type, List<String> into) {
    if (type == null) {
      return;
    }
    String name = NamedTypes.baseName(type);
    if (name != null && declaredTypes.containsKey(name)) {
      into.add(name);
    }
  }
}
