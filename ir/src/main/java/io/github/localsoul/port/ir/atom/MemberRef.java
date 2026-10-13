package io.github.localsoul.port.ir.atom;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import java.util.List;

/**
 * Member-Referenz nach {@code docs/ir-spec.md} Abschnitt 4, zugleich stabile Symbol-ID.
 *
 * <p>Beispiele: {@code Mover::speed}, {@code UnityEngine.Transform::Translate#UnityEngine.Vector3},
 * {@code Foo::.ctor#f32,f32,f32}, {@code Foo::Bar#} (leere Parameterliste), {@code
 * Typ::Name<i32>#string}.
 *
 * @param declaringType voll qualifizierter Name des deklarierenden Typs
 * @param name Member-Name, ein Bezeichner oder {@code .ctor}/{@code .cctor}
 * @param genericArguments Typargumente einer generischen Methode, sonst leer (unveränderlich)
 * @param hasParameterList {@code true}, wenn die Referenz ein {@code #} trägt (Methode oder
 *     Konstruktor)
 * @param parameterTypes Parametertypen; leer, wenn {@code hasParameterList} {@code false} ist
 *     (unveränderlich)
 */
public record MemberRef(
    String declaringType,
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

  /** Liefert die kanonische Schreibweise, die zugleich die stabile Symbol-ID ist. */
  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append(declaringType).append("::").append(name);
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
