package io.github.localsoul.runtime.system;

/**
 * {@code System.Func<T1,T2,T3,T4,TResult>}: Delegate als funktionales Interface.
 *
 * @param <T1> Typ des 1. Parameters
 * @param <T2> Typ des 2. Parameters
 * @param <T3> Typ des 3. Parameters
 * @param <T4> Typ des 4. Parameters
 * @param <R> Rückgabetyp
 */
@FunctionalInterface
public interface Func4<T1, T2, T3, T4, R> {

  /**
   * Ruft den Delegate auf ({@code Invoke}).
   *
   * @param arg1 1. Argument
   * @param arg2 2. Argument
   * @param arg3 3. Argument
   * @param arg4 4. Argument
   * @return Ergebnis
   */
  R invoke(T1 arg1, T2 arg2, T3 arg3, T4 arg4);
}
