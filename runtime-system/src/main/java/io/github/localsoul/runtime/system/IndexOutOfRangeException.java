package io.github.localsoul.runtime.system;

/** {@code System.IndexOutOfRangeException}. */
public class IndexOutOfRangeException extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public IndexOutOfRangeException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public IndexOutOfRangeException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.IndexOutOfRangeException";
  }

  @Override
  protected String defaultMessage() {
    return "Index was outside the bounds of the array.";
  }

  /**
   * Übersetzt eine im generierten {@code catch} gefangene Exception in diesen .NET-Typ (ADR-019).
   *
   * @param caught {@code IndexOutOfRangeException} oder {@link ArrayIndexOutOfBoundsException}
   * @return die .NET-Exception, Ursache ist die Java-Exception
   * @throws RuntimeException {@code caught} selbst bei einem anderen Typ
   */
  public static IndexOutOfRangeException fromJava(RuntimeException caught) {
    if (caught instanceof IndexOutOfRangeException dotnet) {
      return dotnet;
    }
    if (caught instanceof ArrayIndexOutOfBoundsException) {
      IndexOutOfRangeException translated = new IndexOutOfRangeException();
      translated.initCause(caught);
      return translated;
    }
    throw caught;
  }
}
