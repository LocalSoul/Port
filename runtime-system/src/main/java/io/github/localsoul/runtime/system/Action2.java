package io.github.localsoul.runtime.system;

/**
 * {@code System.Action<T1,T2>}: Delegate als funktionales Interface.
 *
 * @param <T1> Typ des 1. Parameters
 * @param <T2> Typ des 2. Parameters
 */
@FunctionalInterface
public interface Action2<T1, T2> {

  /**
   * Ruft den Delegate auf ({@code Invoke}).
   *
   * @param arg1 1. Argument
   * @param arg2 2. Argument
   */
  void invoke(T1 arg1, T2 arg2);
}
