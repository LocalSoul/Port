package io.github.localsoul.port.engine.api.subsystem;

import java.util.List;

/**
 * Ereignisse zwischen Subsystemen und Adaptern, gepuffert pro Phasenschritt (ADR-015, vorläufige
 * Semantik): Was im Phasenschritt k veröffentlicht wird, ist genau im Phasenschritt k+1 sichtbar
 * (auch über Frame-Grenzen: aus {@code LATE} in das {@code EARLY} des nächsten Frames). Die
 * Reihenfolge ist die Veröffentlichungsreihenfolge.
 */
public interface EventBus {

  /**
   * Veröffentlicht ein Ereignis; sichtbar ab dem nächsten Phasenschritt.
   *
   * @param type Ereignistyp
   * @param payload Nutzdaten
   * @throws SubsystemConfigurationException wenn ein Subsystem einen Typ veröffentlicht, den es
   *     nicht in {@link SubsystemInfo#publishes()} deklariert hat
   */
  void publish(EventType type, Object payload);

  /**
   * Liefert die im laufenden Phasenschritt sichtbaren Ereignisse eines Typs.
   *
   * @param type Ereignistyp
   * @return Ereignisse in Veröffentlichungsreihenfolge (unveränderlich)
   * @throws SubsystemConfigurationException wenn ein Subsystem einen Typ liest, den es nicht in
   *     {@link SubsystemInfo#consumes()} deklariert hat
   */
  List<Event> events(EventType type);
}
