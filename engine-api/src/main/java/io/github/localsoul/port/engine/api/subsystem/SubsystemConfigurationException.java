package io.github.localsoul.port.engine.api.subsystem;

import io.github.localsoul.port.engine.api.EngineException;

/**
 * Ungültige Kombination oder Verletzung des Vertrags von Subsystemen (doppelter Schreiber, fehlende
 * Pflichtpaare, Konflikte, undeklarierte Ereignisse). Die Meldung nennt die beteiligten
 * Subsystem-IDs.
 */
public final class SubsystemConfigurationException extends EngineException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit den beteiligten Subsystemen
   */
  public SubsystemConfigurationException(String message) {
    super(message);
  }
}
