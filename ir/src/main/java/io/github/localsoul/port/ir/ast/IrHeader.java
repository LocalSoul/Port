package io.github.localsoul.port.ir.ast;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Dateikopf {@code (ir :version nat :assembly string [:sha256 string])}.
 *
 * @param version IR-Version
 * @param assembly Name der Assembly
 * @param sha256 Prüfsumme der Eingabe, oder {@code null}
 * @param attrs Attribute {@code :loc}, {@code :manual}, {@code :note}
 * @param span Position der öffnenden Klammer bzw. des Atoms
 */
public record IrHeader(
    int version, String assembly, String sha256, NodeAttrs attrs, SourceSpan span)
    implements IrNode {}
