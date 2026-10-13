package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.EnumMember;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Modifier;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.SymbolIds;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * IR002 (hängende Referenz) und IR003 (Typ {@code unknown}).
 *
 * <ul>
 *   <li>Typ in der Datei deklariert oder generischer Typparameter: aufgelöst.
 *   <li>Externer Typ mit Katalog: Fehler IR002, wenn der Katalog ihn nicht kennt.
 *   <li>Externer Typ ohne Katalog: einmal pro Typname {@code info} IR002 („nicht geprüft").
 *   <li>Member-Referenz auf einen Typ der Datei: Das Member muss dort deklariert sein. Bei
 *       generischen Methoden zählen Name, Anzahl der Typparameter und Anzahl der Parameter.
 *   <li>{@code unknown} irgendwo in einem Typ: IR003.
 * </ul>
 */
final class ReferenceRule {

  private final Map<String, TypeDecl> declaredTypes;
  private final TypeCatalog catalog;
  private final DiagnosticSink sink;
  // Nur für Lookups, nie iteriert.
  private final Set<String> reportedExternal = new HashSet<>();

  ReferenceRule(Map<String, TypeDecl> declaredTypes, TypeCatalog catalog, DiagnosticSink sink) {
    this.declaredTypes = declaredTypes;
    this.catalog = catalog;
    this.sink = sink;
  }

  void check(List<Reference> references) {
    for (Reference reference : references) {
      if (reference.type() != null) {
        checkType(reference.type(), reference);
      } else {
        checkMember(reference.member(), reference);
      }
    }
  }

  private void checkType(TypeRef type, Reference reference) {
    for (NamedType named : NamedTypes.in(type)) {
      checkTypeName(named.qualifiedName(), reference);
    }
  }

  private void checkTypeName(String name, Reference reference) {
    if (name.equals(NamedTypes.UNKNOWN)) {
      sink.error(
          "IR003",
          reference,
          "Typ nicht aufgelöst (unknown)",
          "Typ im Frontend oder per Fix ergänzen");
      return;
    }
    if (reference.genericScope().contains(name) || declaredTypes.containsKey(name)) {
      return;
    }
    if (catalog != null) {
      if (!catalog.containsType(name)) {
        sink.error("IR002", reference, "Typ '" + name + "' nicht gefunden", null);
      }
      return;
    }
    if (reportedExternal.add(name)) {
      sink.info(
          "IR002",
          reference,
          "externer Typ '" + name + "' nicht geprüft (kein Typkatalog)",
          "Typkatalog übergeben");
    }
  }

  private void checkMember(MemberRef member, Reference reference) {
    String typeName = member.declaringType();
    checkTypeName(typeName, reference);
    for (TypeRef argument : member.declaringTypeArguments()) {
      checkType(argument, reference);
    }
    for (TypeRef argument : member.genericArguments()) {
      checkType(argument, reference);
    }
    boolean openSignature =
        !member.declaringTypeArguments().isEmpty() || !member.genericArguments().isEmpty();
    for (TypeRef parameter : member.parameterTypes()) {
      checkParameterType(parameter, openSignature, reference);
    }
    TypeDecl declared = declaredTypes.get(typeName);
    if (declared != null && !isDeclared(declared, member)) {
      sink.error(
          "IR002",
          reference,
          "Member '" + member + "' ist in '" + typeName + "' nicht deklariert",
          null);
    }
  }

  /**
   * Parametertypen einer Referenz sind die der Deklaration (ADR-025). Bei generischen Typen oder
   * Methoden enthalten sie deren Typparameter (z. B. {@code T}); einfache Namen ohne Punkt gelten
   * dort als Typparameter und werden nicht aufgelöst.
   */
  private void checkParameterType(TypeRef parameter, boolean openSignature, Reference reference) {
    for (NamedType named : NamedTypes.in(parameter)) {
      boolean looksLikeTypeParameter = named.qualifiedName().indexOf('.') < 0;
      if (openSignature
          && looksLikeTypeParameter
          && !declaredTypes.containsKey(named.qualifiedName())) {
        continue;
      }
      checkTypeName(named.qualifiedName(), reference);
    }
  }

  private static boolean isDeclared(TypeDecl type, MemberRef ref) {
    if (type instanceof EnumDecl enumDecl) {
      return isEnumMember(enumDecl, ref);
    }
    for (MemberDecl member : TypeMembers.of(type)) {
      if (matches(member, ref)) {
        return true;
      }
    }
    return false;
  }

  private static boolean isEnumMember(EnumDecl decl, MemberRef ref) {
    if (ref.hasParameterList()) {
      return false;
    }
    for (EnumMember member : decl.members()) {
      if (member.name().equals(ref.name())) {
        return true;
      }
    }
    return false;
  }

  private static boolean matches(MemberDecl member, MemberRef ref) {
    return switch (member) {
      case FieldDecl f -> !ref.hasParameterList() && f.name().equals(ref.name());
      case PropDecl p -> !ref.hasParameterList() && p.name().equals(ref.name());
      case EventDecl e -> !ref.hasParameterList() && e.name().equals(ref.name());
      case CtorDecl c -> ref.hasParameterList() && ctorMatches(c, ref);
      case MethodDecl m -> ref.hasParameterList() && methodMatches(m, ref);
    };
  }

  private static boolean ctorMatches(CtorDecl ctor, MemberRef ref) {
    boolean isStatic = ctor.modifiers().contains(Modifier.STATIC);
    String name = isStatic ? ".cctor" : ".ctor";
    return name.equals(ref.name()) && sameParameterTypes(ctor.parameters(), ref);
  }

  private static boolean methodMatches(MethodDecl method, MemberRef ref) {
    if (!method.name().equals(ref.name())) {
      return false;
    }
    boolean generic = !method.generics().isEmpty() || !ref.genericArguments().isEmpty();
    if (!generic) {
      return sameParameterTypes(method.parameters(), ref);
    }
    boolean sameArity = method.generics().size() == ref.genericArguments().size();
    boolean sameCount = method.parameters().size() == ref.parameterTypes().size();
    return sameArity && sameCount;
  }

  private static boolean sameParameterTypes(List<Param> params, MemberRef ref) {
    List<TypeRef> refTypes = ref.parameterTypes();
    if (params.size() != refTypes.size()) {
      return false;
    }
    for (int i = 0; i < params.size(); i++) {
      String declared = SymbolIds.parameterType(params.get(i));
      if (!declared.equals(refTypes.get(i).toString())) {
        return false;
      }
    }
    return true;
  }
}
