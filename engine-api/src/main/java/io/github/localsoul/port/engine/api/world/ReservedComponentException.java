package io.github.localsoul.port.engine.api.world;

import io.github.localsoul.port.engine.api.EngineException;

/**
 * Direkter Schreibzugriff auf eine vom Core verwaltete Komponente ({@code Parent}, {@code
 * Children}, {@code WorldTransform}).
 */
public final class ReservedComponentException extends EngineException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit den beteiligten Entities bzw. Typen
   */
  public ReservedComponentException(String message) {
    super(message);
  }
}
