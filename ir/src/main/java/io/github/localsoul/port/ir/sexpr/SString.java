package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * String-Literal.
 *
 * @param value Inhalt mit aufgelösten Escapes
 * @param span Position des öffnenden Anführungszeichens
 */
public record SString(String value, SourceSpan span) implements SExpr {}
