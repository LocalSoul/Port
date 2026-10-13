package io.github.localsoul.runtime.system.collections.generic;

import java.util.Iterator;

/**
 * {@code System.Collections.Generic.HashSet<T>} mit .NET-Reihenfolge (siehe {@link Slots}).
 *
 * @param <T> Elementtyp
 */
public final class HashSet<T> implements Iterable<T> {

  private final Slots<T, Object> slots = new Slots<>();

  /** {@code new HashSet<T>()}. */
  public HashSet() {}

  /**
   * {@code Count}.
   *
   * @return Anzahl
   */
  public int count() {
    return slots.size();
  }

  /**
   * {@code Add}.
   *
   * @param item Element ({@code null} erlaubt wie in .NET)
   * @return {@code false}, wenn schon vorhanden
   */
  public boolean add(T item) {
    if (slots.find(item) >= 0) {
      return false;
    }
    slots.insert(item, null);
    return true;
  }

  /**
   * {@code Remove}.
   *
   * @param item Element
   * @return {@code true}, wenn entfernt
   */
  public boolean remove(T item) {
    return slots.remove(item);
  }

  /**
   * {@code Contains}.
   *
   * @param item Element
   * @return {@code true}, wenn vorhanden
   */
  public boolean contains(T item) {
    return slots.find(item) >= 0;
  }

  /** {@code Clear}. */
  public void clear() {
    slots.clear();
  }

  @Override
  public Iterator<T> iterator() {
    return new SlotIterator<>(slots, slots::keyAt);
  }
}
