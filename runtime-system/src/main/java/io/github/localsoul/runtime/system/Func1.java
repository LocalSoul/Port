package io.github.localsoul.runtime.system;

/**
 * {@code System.Func<T1,TResult>}: Delegate als funktionales Interface.
 *
 * @param <T1> Typ des 1. Parameters
 * @param <R> Rückgabetyp
 */
@FunctionalInterface
public interface Func1<T1, R> {

  /**
   * Ruft den Delegate auf ({@code Invoke}).
   *
   * @param arg1 1. Argument
   * @return Ergebnis
   */
  R invoke(T1 arg1);
}
