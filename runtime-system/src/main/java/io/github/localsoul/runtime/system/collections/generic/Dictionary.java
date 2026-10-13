package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.ArgumentException;
import io.github.localsoul.runtime.system.ArgumentNullException;
import java.util.Iterator;

/**
 * {@code System.Collections.Generic.Dictionary<TKey,TValue>} mit .NET-Semantik (ADR-019):
 * Aufzählung in .NET-Reihenfolge (siehe {@link Slots}), doppelter Schlüssel bei {@code Add} wirft
 * {@link ArgumentException}, fehlender Schlüssel beim Lesen wirft {@link KeyNotFoundException},
 * {@code null}-Schlüssel wirft {@link ArgumentNullException}.
 *
 * @param <K> Schlüsseltyp
 * @param <V> Werttyp
 */
public final class Dictionary<K, V> implements Iterable<KeyValuePair<K, V>> {

  private final Slots<K, V> slots = new Slots<>();

  /** {@code new Dictionary<K,V>()}. */
  public Dictionary() {}

  /**
   * {@code Count}.
   *
   * @return Anzahl der Einträge
   */
  public int count() {
    return slots.size();
  }

  /**
   * {@code Add}.
   *
   * @param key Schlüssel
   * @param value Wert
   * @throws ArgumentException wenn der Schlüssel schon existiert
   * @throws ArgumentNullException bei {@code null}-Schlüssel
   */
  public void add(K key, V value) {
    checkKey(key);
    if (slots.find(key) >= 0) {
      throw new ArgumentException("An item with the same key has already been added.");
    }
    slots.insert(key, value);
  }

  /**
   * Indexer lesen ({@code get_Item}).
   *
   * @param key Schlüssel
   * @return Wert
   * @throws KeyNotFoundException wenn der Schlüssel fehlt
   */
  public V get(K key) {
    checkKey(key);
    int slot = slots.find(key);
    if (slot < 0) {
      throw new KeyNotFoundException("The given key was not present in the dictionary.");
    }
    return slots.valueAt(slot);
  }

  /**
   * Indexer schreiben ({@code set_Item}): ersetzt an derselben Position oder fügt hinzu.
   *
   * @param key Schlüssel
   * @param value Wert
   */
  public void set(K key, V value) {
    checkKey(key);
    int slot = slots.find(key);
    if (slot >= 0) {
      slots.replace(slot, value);
    } else {
      slots.insert(key, value);
    }
  }

  /**
   * {@code Remove}.
   *
   * @param key Schlüssel
   * @return {@code true}, wenn entfernt
   */
  public boolean remove(K key) {
    checkKey(key);
    return slots.remove(key);
  }

  /**
   * {@code ContainsKey}.
   *
   * @param key Schlüssel
   * @return {@code true}, wenn vorhanden
   */
  public boolean containsKey(K key) {
    checkKey(key);
    return slots.find(key) >= 0;
  }

  /** {@code Clear}. */
  public void clear() {
    slots.clear();
  }

  /**
   * {@code Keys}: Sicht in derselben Reihenfolge wie die Aufzählung.
   *
   * @return Schlüssel
   */
  public Iterable<K> keys() {
    return () -> new SlotIterator<>(slots, slots::keyAt);
  }

  /**
   * {@code Values}: Sicht in derselben Reihenfolge wie die Aufzählung.
   *
   * @return Werte
   */
  public Iterable<V> values() {
    return () -> new SlotIterator<>(slots, slots::valueAt);
  }

  @Override
  public Iterator<KeyValuePair<K, V>> iterator() {
    return new SlotIterator<>(
        slots, slot -> new KeyValuePair<>(slots.keyAt(slot), slots.valueAt(slot)));
  }

  private static void checkKey(Object key) {
    if (key == null) {
      throw new ArgumentNullException("Value cannot be null. (Parameter 'key')");
    }
  }
}
