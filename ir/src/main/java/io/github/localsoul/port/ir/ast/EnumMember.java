package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Enum-Wert {@code (ident number)}.
 *
 * @param name Name
 * @param value Wert
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record EnumMember(String name, NumberLit value, NodeAttrs attrs, SourceSpan span)
    implements IrNode {}
