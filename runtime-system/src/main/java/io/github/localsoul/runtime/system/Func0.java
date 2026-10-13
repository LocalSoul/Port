package io.github.localsoul.runtime.system;

/**
 * {@code System.Func<TResult>}: Delegate als funktionales Interface.
 *
 * @param <R> Rückgabetyp
 */
@FunctionalInterface
public interface Func0<R> {

  /**
   * Ruft den Delegate auf ({@code Invoke}).
   *
   * @return Ergebnis
   */
  R invoke();
}
