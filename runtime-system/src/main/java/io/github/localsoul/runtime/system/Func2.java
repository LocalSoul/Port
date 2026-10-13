package io.github.localsoul.runtime.system;

/**
 * {@code System.Func<T1,T2,TResult>}: Delegate als funktionales Interface.
 *
 * @param <T1> Typ des 1. Parameters
 * @param <T2> Typ des 2. Parameters
 * @param <R> Rückgabetyp
 */
@FunctionalInterface
public interface Func2<T1, T2, R> {

  /**
   * Ruft den Delegate auf ({@code Invoke}).
   *
   * @param arg1 1. Argument
   * @param arg2 2. Argument
   * @return Ergebnis
   */
  R invoke(T1 arg1, T2 arg2);
}
