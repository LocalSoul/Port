package io.github.localsoul.runtime.system;

/**
 * {@code System.Action<T1>}: Delegate als funktionales Interface.
 *
 * @param <T1> Typ des 1. Parameters
 */
@FunctionalInterface
public interface Action1<T1> {

  /**
   * Ruft den Delegate auf ({@code Invoke}).
   *
   * @param arg1 1. Argument
   */
  void invoke(T1 arg1);
}
