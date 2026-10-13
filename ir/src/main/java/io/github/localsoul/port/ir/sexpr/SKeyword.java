package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Keyword wie {@code :base}.
 *
 * @param name Name ohne führenden Doppelpunkt, z. B. {@code base-call}
 * @param span Position des Doppelpunkts
 */
public record SKeyword(String name, SourceSpan span) implements SExpr {}
