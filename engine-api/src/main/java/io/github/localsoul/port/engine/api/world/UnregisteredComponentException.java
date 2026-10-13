package io.github.localsoul.port.engine.api.world;

import io.github.localsoul.port.engine.api.EngineException;

/** Der Komponententyp wurde nicht mit {@link World#registerComponent} registriert. */
public final class UnregisteredComponentException extends EngineException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit den beteiligten Entities bzw. Typen
   */
  public UnregisteredComponentException(String message) {
    super(message);
  }
}
