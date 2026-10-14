package io.github.localsoul.port.ir.refs;

import io.github.localsoul.port.ir.SourceSpan;

/**
 * Eine Deklaration einer IR-Datei.
 *
 * @param symbol stabile Symbol-ID ({@code docs/ir-spec.md}, Abschnitt 4)
 * @param kind {@code class}, {@code struct}, {@code interface}, {@code enum}, {@code field}, {@code
 *     prop}, {@code event}, {@code ctor}, {@code method} oder {@code enum-member}
 * @param name einfacher Name, z. B. {@code Update}, {@code .ctor} oder bei Typen der letzte Teil
 * @param type qualifizierter Name des deklarierenden Typs, bei Typen der Typ selbst
 * @param span Position der Deklaration
 */
public record Declaration(String symbol, String kind, String name, String type, SourceSpan span) {}
