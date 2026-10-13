package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.atom.ByRefType;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * Stabile Symbol-IDs von Deklarationen ({@code docs/ir-spec.md} Abschnitt 4, AGENTS.md Regel 6).
 *
 * <ul>
 *   <li>Typ: qualifizierter Name, z. B. {@code UnityEngine.Mover}.
 *   <li>Feld, Property, Event, Enum-Wert: {@code Typ::name}.
 *   <li>Methode: {@code Typ::name#P1,P2}, generisch {@code Typ::name<T,U>#P1}.
 *   <li>Konstruktor: {@code Typ::.ctor#P1,P2}, mit Modifier {@code static} {@code Typ::.cctor#}.
 *   <li>Parameter mit {@code :ref}, {@code :out} oder {@code :in} erscheinen mit Suffix {@code &}
 *       (Spec Abschnitt 3), außer der Typ trägt es schon.
 * </ul>
 */
public final class SymbolIds {

  private SymbolIds() {}

  /**
   * Liefert die ID eines Typs.
   *
   * @param type Typdeklaration
   * @return der qualifizierte Name
   */
  public static String of(TypeDecl type) {
    return type.name();
  }

  /**
   * Liefert die ID eines Mitglieds.
   *
   * @param typeName qualifizierter Name des deklarierenden Typs
   * @param member Mitglied
   * @return die stabile Symbol-ID
   */
  public static String of(String typeName, MemberDecl member) {
    String prefix = typeName + "::";
    return switch (member) {
      case FieldDecl field -> prefix + field.name();
      case PropDecl prop -> prefix + prop.name();
      case EventDecl event -> prefix + event.name();
      case CtorDecl ctor -> prefix + ctorName(ctor) + parameterSuffix(ctor.parameters());
      case MethodDecl method ->
          prefix
              + method.name()
              + genericSuffix(method.generics())
              + parameterSuffix(method.parameters());
    };
  }

  /**
   * Liefert die ID eines Enum-Werts.
   *
   * @param enumName qualifizierter Name des Enums
   * @param member Enum-Wert
   * @return {@code Enum::Name}
   */
  public static String of(String enumName, EnumMember member) {
    return enumName + "::" + member.name();
  }

  /**
   * Liefert den Parametertyp, wie er in IDs steht: mit {@code &} für {@code :ref}, {@code :out},
   * {@code :in}.
   *
   * @param param Parameter
   * @return Typ als Text
   */
  public static String parameterType(Param param) {
    TypeRef type = param.type();
    boolean byRef = param.mode() != ParamMode.VALUE;
    if (byRef && !(type instanceof ByRefType)) {
      return type + "&";
    }
    return type.toString();
  }

  private static String ctorName(CtorDecl ctor) {
    if (ctor.modifiers().contains(Modifier.STATIC)) {
      return ".cctor";
    }
    return ".ctor";
  }

  private static String genericSuffix(List<String> generics) {
    if (generics.isEmpty()) {
      return "";
    }
    return "<" + String.join(",", generics) + ">";
  }

  private static String parameterSuffix(List<Param> parameters) {
    StringBuilder out = new StringBuilder("#");
    for (int i = 0; i < parameters.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(parameterType(parameters.get(i)));
    }
    return out.toString();
  }
}
