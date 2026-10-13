package io.github.localsoul.port.ir;

/**
 * Syntaxfehler beim Lesen von IR-Text (Diagnose-ID IR001).
 *
 * <p>Die Meldung hat die Form {@code datei:zeile:spalte: Grund}.
 */
public final class IrSyntaxException extends IrException {

  private static final long serialVersionUID = 1L;

  private final SourceSpan span;
  private final String reason;

  /**
   * Erzeugt einen Syntaxfehler.
   *
   * @param span Position, an der der Fehler erkannt wurde bzw. an der das fehlerhafte Konstrukt
   *     beginnt
   * @param reason Ursache ohne Positionsangabe
   */
  public IrSyntaxException(SourceSpan span, String reason) {
    super(span + ": " + reason);
    this.span = span;
    this.reason = reason;
  }

  /** Liefert die Position des Fehlers. */
  public SourceSpan span() {
    return span;
  }

  /** Liefert die Ursache ohne Positionsangabe. */
  public String reason() {
    return reason;
  }
}
