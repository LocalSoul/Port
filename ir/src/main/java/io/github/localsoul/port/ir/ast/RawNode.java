package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * {@code (raw cs|asm string)}, nicht übersetzter Originaltext (Spec Abschnitt 9).
 *
 * @param language Sprache des Textes
 * @param text Originaltext
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record RawNode(RawLanguage language, String text, NodeAttrs attrs, SourceSpan span)
    implements Expr, Stmt {}
