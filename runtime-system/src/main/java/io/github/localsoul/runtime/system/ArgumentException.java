package io.github.localsoul.runtime.system;

/**
 * {@code System.ArgumentException}. Mit Parametername lautet {@code Message} wie in .NET Framework
 * / Unity-Mono „Meldung“ + Zeilenumbruch + „Parameter name: x“ (Zeilenumbruch hier immer {@code
 * \n}, ADR-019).
 */
public class ArgumentException extends SystemException {

  private static final long serialVersionUID = 1L;

  private final String paramName;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public ArgumentException() {
    this(null, null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public ArgumentException(String message) {
    this(message, null);
  }

  /**
   * Ausnahme mit Meldung und Parametername (Reihenfolge wie in .NET).
   *
   * @param message Meldung; {@code null} ergibt die .NET-Standardmeldung
   * @param paramName Name des fehlerhaften Parameters oder {@code null}
   */
  public ArgumentException(String message, String paramName) {
    super(message);
    this.paramName = paramName;
  }

  /**
   * {@code ParamName}.
   *
   * @return Parametername oder {@code null}
   */
  public String paramName() {
    return paramName;
  }

  @Override
  public String message() {
    String text = super.message();
    if (paramName == null || paramName.isEmpty()) {
      return text;
    }
    return text + "\nParameter name: " + paramName;
  }

  @Override
  protected String dotnetName() {
    return "System.ArgumentException";
  }

  @Override
  protected String defaultMessage() {
    return "Value does not fall within the expected range.";
  }
}
