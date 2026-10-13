package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.TypeRef;

/**
 * Parameter {@code (ident type [:ref|:out|:in] [:default expr])}.
 *
 * @param name Name
 * @param type Typ
 * @param mode Übergabeart
 * @param defaultValue Standardwert, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record Param(
    String name, TypeRef type, ParamMode mode, Expr defaultValue, NodeAttrs attrs, SourceSpan span)
    implements IrNode {}
