package io.github.localsoul.port.frontend.unity;

/**
 * Ergebnis eines Programmlaufs.
 *
 * @param exitCode Exit-Code des Programms
 * @param stderr gesamte Ausgabe auf stderr (Zusammenfassung, raw-Stellen)
 */
public record ToolResult(int exitCode, String stderr) {}
