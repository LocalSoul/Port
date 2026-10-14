package io.github.localsoul.port.index;

/**
 * Ein Symbol im Index.
 *
 * @param symbol Symbol-ID
 * @param kind Art ({@code class}, {@code method}, ...)
 * @param name einfacher Name
 * @param type deklarierender Typ (bei Typen der Typ selbst)
 * @param location {@code ir/Datei.ir:zeile:spalte}
 */
public record SymbolRow(String symbol, String kind, String name, String type, String location) {}
