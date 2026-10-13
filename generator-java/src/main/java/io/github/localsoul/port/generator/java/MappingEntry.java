package io.github.localsoul.port.generator.java;

/**
 * Eine Zeile von {@code mapping.tsv}.
 *
 * @param irSymbol stabile Symbol-ID (ADR-025, ohne Typargumente am Typ) oder qualifizierter Typname
 * @param javaSymbol Java-Name: Klassenname bei Typen, sonst Member-Name
 * @param form Form
 */
public record MappingEntry(String irSymbol, String javaSymbol, MappingForm form) {}
