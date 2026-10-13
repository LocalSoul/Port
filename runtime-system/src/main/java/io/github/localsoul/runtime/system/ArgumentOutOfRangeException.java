package io.github.localsoul.runtime.system;

/**
 * {@code System.ArgumentOutOfRangeException}. Wie in .NET ist das einzelne String-Argument der
 * Parametername, nicht die Meldung.
 */
public class ArgumentOutOfRangeException extends ArgumentException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public ArgumentOutOfRangeException() {
    super(null, null);
  }

  /**
   * Ausnahme für einen Parameter, Meldung ist die .NET-Standardmeldung.
   *
   * @param paramName Name des fehlerhaften Parameters
   */
  public ArgumentOutOfRangeException(String paramName) {
    super(null, paramName);
  }

  /**
   * Ausnahme für einen Parameter mit Meldung (Reihenfolge wie in .NET).
   *
   * @param paramName Name des fehlerhaften Parameters
   * @param message Meldung; {@code null} ergibt die .NET-Standardmeldung
   */
  public ArgumentOutOfRangeException(String paramName, String message) {
    super(message, paramName);
  }

  @Override
  protected String dotnetName() {
    return "System.ArgumentOutOfRangeException";
  }

  @Override
  protected String defaultMessage() {
    return "Specified argument was out of the range of valid values.";
  }
}
