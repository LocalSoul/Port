package io.github.localsoul.port.generator.java;

/** Ungültige {@code mapping.tsv}; die Meldung nennt Zeile und Grund. */
public final class MappingException extends Exception {

  private static final long serialVersionUID = 1L;

  /**
   * Erzeugt den Fehler.
   *
   * @param message Meldung mit Zeilennummer
   */
  public MappingException(String message) {
    super(message);
  }
}
