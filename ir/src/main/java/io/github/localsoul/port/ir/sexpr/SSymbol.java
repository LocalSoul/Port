package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Atom ohne besondere Lexik: Bezeichner, Typ, Member-Referenz, Operator, {@code true}, {@code
 * null}, ...
 *
 * @param text Text des Atoms
 * @param span Position
 */
public record SSymbol(String text, SourceSpan span) implements SExpr {}
