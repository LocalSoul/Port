package io.github.localsoul.port.engine.api.subsystem;

import java.util.Set;

/**
 * Deklaration eines Subsystems (ADR-015). Die Engine prüft beim Start: IDs eindeutig, höchstens ein
 * Schreiber je Komponente (vom Core verwaltete Komponenten zählen als belegt), alle {@code
 * requires} erfüllt, keine {@code conflictsWith} verletzt.
 *
 * @param id eindeutige ID, z. B. {@code input-scripted}
 * @param kind Art, z. B. {@code input}, {@code render}, {@code physics}, {@code audio}
 * @param reads gelesene Komponenten
 * @param writes geschriebene Komponenten (Besitz)
 * @param publishes gesendete Ereignisse
 * @param consumes empfangene Ereignisse
 * @param requires IDs oder Arten anderer Subsysteme, die vorhanden sein müssen
 * @param conflictsWith IDs oder Arten anderer Subsysteme, die nicht vorhanden sein dürfen
 */
public record SubsystemInfo(
    String id,
    String kind,
    Set<ComponentType> reads,
    Set<ComponentType> writes,
    Set<EventType> publishes,
    Set<EventType> consumes,
    Set<String> requires,
    Set<String> conflictsWith) {

  /** Kopiert alle Mengen (unveränderlich; Iterationsreihenfolge nicht festgelegt). */
  public SubsystemInfo {
    reads = Set.copyOf(reads);
    writes = Set.copyOf(writes);
    publishes = Set.copyOf(publishes);
    consumes = Set.copyOf(consumes);
    requires = Set.copyOf(requires);
    conflictsWith = Set.copyOf(conflictsWith);
  }
}
