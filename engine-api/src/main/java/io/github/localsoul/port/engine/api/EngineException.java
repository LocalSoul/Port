package io.github.localsoul.port.engine.api;

/** Gemeinsame Basis aller Fehler der Engine (ungeprüft, Programmierfehler im Aufrufer). */
public class EngineException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt einen Fehler mit Meldung.
   *
   * @param message Meldung
   */
  public EngineException(String message) {
    super(message);
  }
}
