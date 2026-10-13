package io.github.localsoul.port.ir.atom;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * Member-Referenz nach {@code docs/ir-spec.md} Abschnitt 4.
 *
 * <p>Beispiele: {@code Mover::speed}, {@code UnityEngine.Transform::Translate#UnityEngine.Vector3},
 * {@code Foo::.ctor#f32,f32,f32}, {@code Foo::Bar#} (leere Parameterliste), {@code
 * Typ::Name<i32>#string}, {@code System.Collections.Generic.List<UnityEngine.Transform>::Add#T}
 * (Member einer generischen Typinstanz, ADR-025).
 *
 * <p>Die Parametertypen sind immer die der Deklaration (offen, z. B. {@code T}). Die stabile
 * Symbol-ID ist {@link #symbolId()}: die Referenz ohne Typargumente am deklarierenden Typ.
 *
 * @param declaringType voll qualifizierter Name des deklarierenden Typs, ohne Typargumente
 * @param declaringTypeArguments Typargumente der Instanz des deklarierenden Typs, sonst leer
 *     (unveränderlich)
 * @param name Member-Name, ein Bezeichner oder {@code .ctor}/{@code .cctor}
 * @param genericArguments Typargumente einer generischen Methode, sonst leer (unveränderlich)
 * @param hasParameterList {@code true}, wenn die Referenz ein {@code #} trägt (Methode oder
 *     Konstruktor)
 * @param parameterTypes Parametertypen; leer, wenn {@code hasParameterList} {@code false} ist
 *     (unveränderlich)
 */
public record MemberRef(
    String declaringType,
    List<TypeRef> declaringTypeArguments,
    String name,
    List<TypeRef> genericArguments,
    boolean hasParameterList,
    List<TypeRef> parameterTypes) {

  /**
   * Kopiert die Listen und prüft die Konsistenz.
   *
   * @throws IllegalArgumentException wenn Parametertypen ohne Parameterliste angegeben sind
   */
  public MemberRef {
    declaringTypeArguments = List.copyOf(declaringTypeArguments);
    genericArguments = List.copyOf(genericArguments);
    parameterTypes = List.copyOf(parameterTypes);
    if (!hasParameterList && !parameterTypes.isEmpty()) {
      throw new IllegalArgumentException("Parametertypen ohne Parameterliste: " + parameterTypes);
    }
  }

  /**
   * Parst eine Member-Referenz.
   *
   * @param text Member-Atom, z. B. {@code Foo::.ctor#f32,f32,f32}
   * @param atomStart Position des ersten Zeichens von {@code text}
   * @return die Member-Referenz
   * @throws IrSyntaxException wenn {@code text} keine gültige Member-Referenz ist; Position zeigt
   *     auf die Fehlerstelle im Atom
   */
  public static MemberRef parse(String text, SourceSpan atomStart) throws IrSyntaxException {
    AtomParser parser = new AtomParser(text, atomStart);
    return parser.parseWholeMember();
  }

  /**
   * Member-Referenz ohne Typargumente, z. B. für nicht-generische Typen.
   *
   * @param declaringType qualifizierter Name des deklarierenden Typs
   * @param name Member-Name
   * @param genericArguments Typargumente einer generischen Methode
   * @param hasParameterList {@code true} bei Methoden und Konstruktoren
   * @param parameterTypes Parametertypen der Deklaration
   * @return Member-Referenz
   */
  public static MemberRef of(
      String declaringType,
      String name,
      List<TypeRef> genericArguments,
      boolean hasParameterList,
      List<TypeRef> parameterTypes) {
    return new MemberRef(
        declaringType, List.of(), name, genericArguments, hasParameterList, parameterTypes);
  }

  /**
   * Liefert die stabile Symbol-ID (ADR-025): die Referenz ohne Typargumente am deklarierenden Typ,
   * z. B. {@code System.Collections.Generic.List::Add#T}.
   *
   * @return Symbol-ID
   */
  public String symbolId() {
    return render(false);
  }

  /** Liefert die kanonische Schreibweise der Referenz, inklusive Typargumenten am Typ. */
  @Override
  public String toString() {
    return render(true);
  }

  private String render(boolean withDeclaringTypeArguments) {
    StringBuilder out = new StringBuilder();
    out.append(declaringType);
    if (withDeclaringTypeArguments) {
      AtomParser.appendTypeList(out, '<', declaringTypeArguments, '>');
    }
    out.append("::").append(name);
    AtomParser.appendTypeList(out, '<', genericArguments, '>');
    if (hasParameterList) {
      out.append('#');
      for (int i = 0; i < parameterTypes.size(); i++) {
        if (i > 0) {
          out.append(',');
        }
        out.append(parameterTypes.get(i));
      }
    }
    return out.toString();
  }
}
