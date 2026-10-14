package io.github.localsoul.port.index;

/**
 * Änderung eines Symbols beim Indexlauf.
 *
 * @param symbol Symbol-ID
 * @param change {@code added}, {@code changed} oder {@code removed}
 */
public record Change(String symbol, String change) {}
