package io.github.localsoul.runtime.system;

/** {@code System.NullReferenceException}. */
public class NullReferenceException extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public NullReferenceException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public NullReferenceException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.NullReferenceException";
  }
}
