package io.github.localsoul.runtime.system;

/** {@code System.NotSupportedException}. */
public class NotSupportedException extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public NotSupportedException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public NotSupportedException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.NotSupportedException";
  }
}
