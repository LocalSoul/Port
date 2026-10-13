package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Zahl mit Typsuffix, z. B. {@code 5.0f32} oder {@code 0x7FFE0000u64}.
 *
 * <p>Der Wertebereich wird hier nicht geprüft, das übernimmt die AST-Ebene.
 *
 * @param text Text wie in der Eingabe, inklusive Suffix
 * @param suffix Typsuffix
 * @param span Position
 */
public record SNumber(String text, NumberSuffix suffix, SourceSpan span) implements SExpr {}
