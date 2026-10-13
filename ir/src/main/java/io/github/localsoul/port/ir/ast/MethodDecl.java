package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.List;

/**
 * {@code (method ident type params [:mods] [:generics] (body | :abstract))}.
 *
 * @param name Name
 * @param returnType Rückgabetyp
 * @param parameters Parameter
 * @param modifiers Modifier
 * @param generics generische Typparameter
 * @param body Rumpf, {@code null} bei {@code :abstract}, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record MethodDecl(
    String name,
    TypeRef returnType,
    List<Param> parameters,
    List<Modifier> modifiers,
    List<String> generics,
    Stmt body,
    NodeAttrs attrs,
    SourceSpan span)
    implements MemberDecl {

  /** Kopiert Listen in unveränderliche Listen. */
  public MethodDecl {
    parameters = List.copyOf(parameters);
    modifiers = List.copyOf(modifiers);
    generics = List.copyOf(generics);
  }
}
