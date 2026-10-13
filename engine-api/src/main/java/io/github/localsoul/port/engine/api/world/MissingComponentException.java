package io.github.localsoul.port.engine.api.world;

import io.github.localsoul.port.engine.api.EngineException;

/** Die Entity hat keine Komponente des verlangten Typs. */
public final class MissingComponentException extends EngineException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit den beteiligten Entities bzw. Typen
   */
  public MissingComponentException(String message) {
    super(message);
  }
}
