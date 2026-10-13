package io.github.localsoul.port.engine.api.world;

import io.github.localsoul.port.engine.api.EngineException;

/** Die Entity hat bereits eine Komponente dieses Typs. */
public final class DuplicateComponentException extends EngineException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit den beteiligten Entities bzw. Typen
   */
  public DuplicateComponentException(String message) {
    super(message);
  }
}
