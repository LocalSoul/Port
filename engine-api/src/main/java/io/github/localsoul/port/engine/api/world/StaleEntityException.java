package io.github.localsoul.port.engine.api.world;

import io.github.localsoul.port.engine.api.EngineException;

/** Zugriff auf eine Entity, die nicht (mehr) lebt (falsche Generation oder zerstört). */
public final class StaleEntityException extends EngineException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit den beteiligten Entities bzw. Typen
   */
  public StaleEntityException(String message) {
    super(message);
  }
}
