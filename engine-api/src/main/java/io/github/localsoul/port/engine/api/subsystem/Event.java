package io.github.localsoul.port.engine.api.subsystem;

/**
 * Ein veröffentlichtes Ereignis.
 *
 * @param type Ereignistyp
 * @param payload Nutzdaten (unveränderlicher Wert empfohlen)
 * @param sequence fortlaufende Nummer über alle Ereignisse, bestimmt die Reihenfolge
 */
public record Event(EventType type, Object payload, long sequence) {}
