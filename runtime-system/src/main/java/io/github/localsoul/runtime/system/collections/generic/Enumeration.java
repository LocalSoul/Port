package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.InvalidOperationException;

/** Gemeinsame Meldungen und Prüfungen der .NET-Enumeratoren. */
final class Enumeration {

  private Enumeration() {}

  /** Wirft wie .NET, wenn die Collection während der Aufzählung geändert wurde. */
  static void checkVersion(int expected, int actual) {
    if (expected != actual) {
      throw new InvalidOperationException(
          "Collection was modified; enumeration operation may not execute.");
    }
  }
}
