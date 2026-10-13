package io.github.localsoul.runtime.system;

/** {@code System.OverflowException}. */
public class OverflowException extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public OverflowException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public OverflowException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.OverflowException";
  }
}
