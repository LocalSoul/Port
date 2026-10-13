package io.github.localsoul.port.engine.api.world;

import io.github.localsoul.port.engine.api.EngineException;

/** Ungültige Eltern-Kind-Beziehung, z. B. ein Zyklus. */
public final class HierarchyException extends EngineException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit den beteiligten Entities bzw. Typen
   */
  public HierarchyException(String message) {
    super(message);
  }
}
