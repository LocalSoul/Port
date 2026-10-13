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

  @Override
  protected String defaultMessage() {
    return "Object reference not set to an instance of an object.";
  }

  /**
   * Übersetzt eine im generierten {@code catch} gefangene Exception in diesen .NET-Typ (ADR-019).
   *
   * @param caught {@code NullReferenceException} oder {@link NullPointerException}
   * @return die .NET-Exception, Ursache ist die Java-Exception
   * @throws RuntimeException {@code caught} selbst bei einem anderen Typ
   */
  public static NullReferenceException fromJava(RuntimeException caught) {
    if (caught instanceof NullReferenceException dotnet) {
      return dotnet;
    }
    if (caught instanceof NullPointerException) {
      NullReferenceException translated = new NullReferenceException();
      translated.initCause(caught);
      return translated;
    }
    throw caught;
  }
}
