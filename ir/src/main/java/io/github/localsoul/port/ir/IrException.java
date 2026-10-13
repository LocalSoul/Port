package io.github.localsoul.port.ir;

/** Gemeinsame Basis aller Fehler des Moduls {@code ir}. */
public class IrException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt einen Fehler mit Meldung.
   *
   * @param message Fehlermeldung
   */
  public IrException(String message) {
    super(message);
  }
}
