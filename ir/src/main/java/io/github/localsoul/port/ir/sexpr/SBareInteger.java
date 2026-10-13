package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Nicht-negative Ganzzahl ohne Typsuffix, z. B. in {@code (char 97)} oder {@code :version 1}.
 *
 * <p>Die AST-Ebene lehnt sie an allen anderen Stellen als „Zahl ohne Typsuffix" ab.
 *
 * @param text Ziffern wie in der Eingabe
 * @param span Position
 */
public record SBareInteger(String text, SourceSpan span) implements SExpr {}
