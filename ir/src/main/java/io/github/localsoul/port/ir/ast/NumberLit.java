package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.sexpr.NumberSuffix;

/**
 * Zahl mit Typsuffix, z. B. {@code 5.0f32}.
 *
 * @param text Text wie in der Eingabe, inklusive Suffix
 * @param suffix Typsuffix
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record NumberLit(String text, NumberSuffix suffix, NodeAttrs attrs, SourceSpan span)
    implements Expr {}
