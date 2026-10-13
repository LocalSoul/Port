package io.github.localsoul.runtime.system;

/**
 * {@code System.ArgumentNullException}. Wie in .NET ist das einzelne String-Argument der
 * Parametername, nicht die Meldung.
 */
public class ArgumentNullException extends ArgumentException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public ArgumentNullException() {
    super(null, null);
  }

  /**
   * Ausnahme für einen Parameter, Meldung ist die .NET-Standardmeldung.
   *
   * @param paramName Name des fehlerhaften Parameters
   */
  public ArgumentNullException(String paramName) {
    super(null, paramName);
  }

  /**
   * Ausnahme für einen Parameter mit Meldung (Reihenfolge wie in .NET).
   *
   * @param paramName Name des fehlerhaften Parameters
   * @param message Meldung; {@code null} ergibt die .NET-Standardmeldung
   */
  public ArgumentNullException(String paramName, String message) {
    super(message, paramName);
  }

  @Override
  protected String dotnetName() {
    return "System.ArgumentNullException";
  }

  @Override
  protected String defaultMessage() {
    return "Value cannot be null.";
  }
}
