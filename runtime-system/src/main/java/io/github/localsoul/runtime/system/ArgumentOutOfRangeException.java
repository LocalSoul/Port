package io.github.localsoul.runtime.system;

/** {@code System.ArgumentOutOfRangeException}. */
public class ArgumentOutOfRangeException extends ArgumentException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public ArgumentOutOfRangeException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public ArgumentOutOfRangeException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.ArgumentOutOfRangeException";
  }
}
