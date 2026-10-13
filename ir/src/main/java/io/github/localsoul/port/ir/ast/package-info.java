/**
 * AST der IR nach {@code docs/ir-spec.md} Abschnitte 5 bis 7 und 9.
 *
 * <p>Knoten sind Records, benannt nach dem IR-Schlüsselwort ({@code (if ...)} → {@link
 * io.github.localsoul.port.ir.ast.IfStmt}). Jeder Knoten trägt eine Quellposition und die Attribute
 * {@code :loc :manual :note}. Optionale Teile sind {@code null}, Listen sind nie {@code null}
 * (außer wo dokumentiert) und unveränderlich.
 */
package io.github.localsoul.port.ir.ast;
