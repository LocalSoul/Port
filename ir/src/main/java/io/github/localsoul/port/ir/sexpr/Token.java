package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Ein Token des {@link IrLexer}.
 *
 * @param kind Art des Tokens
 * @param text Text, wie er in der Eingabe steht (bei Strings inklusive Anführungszeichen)
 * @param value aufbereiteter Wert: aufgelöster String, Keyword-Name ohne {@code :}, sonst gleich
 *     {@code text}
 * @param suffix Typsuffix bei {@link TokenKind#NUMBER}, sonst {@code null}
 * @param span Position des ersten Zeichens
 */
public record Token(
    TokenKind kind, String text, String value, NumberSuffix suffix, SourceSpan span) {}
