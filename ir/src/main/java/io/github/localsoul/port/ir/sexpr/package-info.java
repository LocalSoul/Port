/**
 * Lexikalische Ebene der IR: Tokens nach {@code docs/ir-spec.md} Abschnitt 2 und ein Reader, der
 * daraus eine Listen-/Atom-Struktur ({@link io.github.localsoul.port.ir.sexpr.SExpr}) mit
 * Quellpositionen baut.
 *
 * <p>Diese Ebene kennt keine Knotennamen. Typ- und Member-Atome bleiben hier unzerlegte Symbole.
 */
package io.github.localsoul.port.ir.sexpr;
