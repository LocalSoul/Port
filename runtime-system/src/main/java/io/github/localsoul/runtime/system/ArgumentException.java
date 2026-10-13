package io.github.localsoul.runtime.system;

/** {@code System.ArgumentException}. */
public class ArgumentException extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public ArgumentException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public ArgumentException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.ArgumentException";
  }
}
