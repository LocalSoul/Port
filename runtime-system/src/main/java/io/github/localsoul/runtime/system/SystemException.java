package io.github.localsoul.runtime.system;

/**
 * {@code System.Exception} und {@code System.SystemException}: Basis aller nachgebildeten .NET-
 * Exceptions (ADR-019). Erbt von {@link RuntimeException}, damit Java keine {@code throws} verlangt
 * (C# kennt keine geprüften Exceptions).
 */
public class SystemException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  /** Meldung wie übergeben; {@code null} bedeutet .NET-Standardmeldung. */
  private final String explicitMessage;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public SystemException() {
    this(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public SystemException(String message) {
    super(message);
    this.explicitMessage = message;
  }

  /**
   * Voll qualifizierter .NET-Name für die Standardmeldung.
   *
   * @return z. B. {@code System.Exception}
   */
  protected String dotnetName() {
    return "System.Exception";
  }

  /**
   * {@code Exception.Message}: übergebene Meldung oder .NET-Standardmeldung „Exception of type 'X'
   * was thrown.“.
   *
   * @return Meldung, nie {@code null}
   */
  public String message() {
    if (explicitMessage != null) {
      return explicitMessage;
    }
    return defaultMessage();
  }

  /**
   * .NET-Standardmeldung, wenn keine Meldung übergeben wurde.
   *
   * @return „Exception of type 'X' was thrown.“, Unterklassen können abweichen
   */
  protected String defaultMessage() {
    return "Exception of type '" + dotnetName() + "' was thrown.";
  }

  /**
   * Übersetzt eine im generierten {@code catch (System.Exception)} gefangene Exception in den
   * .NET-Typ: {@link NullPointerException} wird {@link NullReferenceException}, {@link
   * ArrayIndexOutOfBoundsException} wird {@link IndexOutOfRangeException} (ADR-019).
   *
   * @param caught gefangene Exception
   * @return die .NET-Exception
   * @throws RuntimeException {@code caught} selbst, wenn es keine passende Übersetzung gibt
   */
  public static SystemException fromJava(RuntimeException caught) {
    if (caught instanceof SystemException system) {
      return system;
    }
    if (caught instanceof NullPointerException) {
      return NullReferenceException.fromJava(caught);
    }
    if (caught instanceof ArrayIndexOutOfBoundsException) {
      return IndexOutOfRangeException.fromJava(caught);
    }
    throw caught;
  }

  @Override
  public String getMessage() {
    return message();
  }
}
