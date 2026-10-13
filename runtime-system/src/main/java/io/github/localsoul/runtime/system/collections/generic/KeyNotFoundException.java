package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.SystemException;

/** {@code System.Collections.Generic.KeyNotFoundException}. */
public class KeyNotFoundException extends SystemException {

  private static final long serialVersionUID = 1L;

  /** Ausnahme mit der .NET-Standardmeldung. */
  public KeyNotFoundException() {
    super(null);
  }

  /**
   * Ausnahme mit Meldung.
   *
   * @param message Meldung ({@code Message}); {@code null} ergibt die .NET-Standardmeldung
   */
  public KeyNotFoundException(String message) {
    super(message);
  }

  @Override
  protected String dotnetName() {
    return "System.Collections.Generic.KeyNotFoundException";
  }

  @Override
  protected String defaultMessage() {
    return "The given key was not present in the dictionary.";
  }
}
