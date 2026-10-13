package io.github.localsoul.runtime.system;

/** {@code System.InvalidOperationException}. */
public class InvalidOperationException extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public InvalidOperationException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public InvalidOperationException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.InvalidOperationException";
  }
}
