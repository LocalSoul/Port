package io.github.localsoul.port.ir.refs;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.Set;

/**
 * Eine Verwendung eines Typs oder Members im AST.
 *
 * @param type verwendeter Typ, oder {@code null} bei Member-Referenzen
 * @param member verwendetes Member, oder {@code null} bei Typreferenzen
 * @param span Position des Knotens, der die Referenz enthält
 * @param symbol ID des umgebenden Typs oder Mitglieds
 * @param genericScope sichtbare generische Typparameter (Typ und Methode)
 */
public record Reference(
    TypeRef type, MemberRef member, SourceSpan span, String symbol, Set<String> genericScope) {}
