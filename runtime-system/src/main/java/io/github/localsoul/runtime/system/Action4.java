package io.github.localsoul.runtime.system;

/**
 * {@code System.Action<T1,T2,T3,T4>}: Delegate als funktionales Interface.
 *
 * @param <T1> Typ des 1. Parameters
 * @param <T2> Typ des 2. Parameters
 * @param <T3> Typ des 3. Parameters
 * @param <T4> Typ des 4. Parameters
 */
@FunctionalInterface
public interface Action4<T1, T2, T3, T4> {

  /**
   * Ruft den Delegate auf ({@code Invoke}).
   *
   * @param arg1 1. Argument
   * @param arg2 2. Argument
   * @param arg3 3. Argument
   * @param arg4 4. Argument
   */
  void invoke(T1 arg1, T2 arg2, T3 arg3, T4 arg4);
}
