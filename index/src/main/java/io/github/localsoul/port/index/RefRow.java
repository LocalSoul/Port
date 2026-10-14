package io.github.localsoul.port.index;

/**
 * Eine Verwendung im Index.
 *
 * @param source Symbol, in dem die Verwendung steht
 * @param target verwendeter Typ oder verwendetes Member (Symbol-ID)
 * @param kind {@code type}, {@code member} (Feld, Property, Event) oder {@code call}
 * @param location {@code ir/Datei.ir:zeile:spalte}
 * @param external {@code true}, wenn das Ziel nicht im Projekt deklariert ist
 */
public record RefRow(
    String source, String target, String kind, String location, boolean external) {}
