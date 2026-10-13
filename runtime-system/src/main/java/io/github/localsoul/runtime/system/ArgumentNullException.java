package io.github.localsoul.runtime.system;

/** {@code System.ArgumentNullException}. */
public class ArgumentNullException extends ArgumentException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public ArgumentNullException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public ArgumentNullException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.ArgumentNullException";
  }
}
