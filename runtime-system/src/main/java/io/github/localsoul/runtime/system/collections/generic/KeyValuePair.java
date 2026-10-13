package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.Convert;

/**
 * {@code System.Collections.Generic.KeyValuePair<TKey,TValue>}.
 *
 * @param <K> Schlüsseltyp
 * @param <V> Werttyp
 */
public final class KeyValuePair<K, V> {

  private final K key;
  private final V value;

  /**
   * {@code new KeyValuePair<K,V>(key, value)}.
   *
   * @param key Schlüssel
   * @param value Wert
   */
  public KeyValuePair(K key, V value) {
    this.key = key;
    this.value = value;
  }

  /**
   * {@code Key}.
   *
   * @return Schlüssel
   */
  public K key() {
    return key;
  }

  /**
   * {@code Value}.
   *
   * @return Wert
   */
  public V value() {
    return value;
  }

  /** .NET-Format {@code [key, value]}. */
  @Override
  public String toString() {
    return "[" + Convert.toString(key) + ", " + Convert.toString(value) + "]";
  }
}
