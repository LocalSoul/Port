package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.ArgumentOutOfRangeException;
import io.github.localsoul.runtime.system.Equality;
import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * {@code System.Collections.Generic.List<T>} mit .NET-Semantik: Indexer und {@code RemoveAt}
 * außerhalb des Bereichs werfen {@link ArgumentOutOfRangeException}, eine Änderung während {@code
 * foreach} wirft {@code InvalidOperationException}, Gleichheit wie {@code
 * EqualityComparer<T>.Default}.
 *
 * @param <T> Elementtyp
 */
public final class List<T> implements Iterable<T> {

  private Object[] items;
  private int size;
  private int version;

  /** {@code new List<T>()}. */
  public List() {
    this.items = new Object[4];
  }

  /**
   * {@code new List<T>(capacity)}.
   *
   * @param capacity Anfangskapazität
   * @throws ArgumentOutOfRangeException bei negativer Kapazität
   */
  public List(int capacity) {
    if (capacity < 0) {
      throw new ArgumentOutOfRangeException("capacity", "Non-negative number required.");
    }
    this.items = new Object[java.lang.Math.max(capacity, 1)];
  }

  /**
   * {@code Count}.
   *
   * @return Anzahl der Elemente
   */
  public int count() {
    return size;
  }

  /**
   * Indexer lesen ({@code get_Item}).
   *
   * @param index Index
   * @return Element
   * @throws ArgumentOutOfRangeException außerhalb von {@code 0..Count-1}
   */
  @SuppressWarnings("unchecked") // nur T wird gespeichert
  public T get(int index) {
    checkIndex(index);
    return (T) items[index];
  }

  /**
   * Indexer schreiben ({@code set_Item}).
   *
   * @param index Index
   * @param value Wert
   * @throws ArgumentOutOfRangeException außerhalb von {@code 0..Count-1}
   */
  public void set(int index, T value) {
    checkIndex(index);
    items[index] = value;
    version++;
  }

  /**
   * {@code Add}.
   *
   * @param item Element
   */
  public void add(T item) {
    ensureCapacity(size + 1);
    items[size] = item;
    size++;
    version++;
  }

  /**
   * {@code Insert}.
   *
   * @param index Position {@code 0..Count}
   * @param item Element
   * @throws ArgumentOutOfRangeException außerhalb von {@code 0..Count}
   */
  public void insert(int index, T item) {
    if (index < 0 || index > size) {
      throw new ArgumentOutOfRangeException(
          "index", "Index must be within the bounds of the List.");
    }
    ensureCapacity(size + 1);
    System.arraycopy(items, index, items, index + 1, size - index);
    items[index] = item;
    size++;
    version++;
  }

  /**
   * {@code RemoveAt}.
   *
   * @param index Index
   * @throws ArgumentOutOfRangeException außerhalb von {@code 0..Count-1}
   */
  public void removeAt(int index) {
    checkIndex(index);
    System.arraycopy(items, index + 1, items, index, size - index - 1);
    size--;
    items[size] = null;
    version++;
  }

  /**
   * {@code Remove}: entfernt das erste gleiche Element.
   *
   * @param item Element
   * @return {@code true}, wenn etwas entfernt wurde
   */
  public boolean remove(T item) {
    int index = indexOf(item);
    if (index < 0) {
      return false;
    }
    removeAt(index);
    return true;
  }

  /**
   * {@code IndexOf}.
   *
   * @param item Element
   * @return erster Index oder −1
   */
  public int indexOf(T item) {
    for (int i = 0; i < size; i++) {
      if (Equality.equals(items[i], item)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * {@code Contains}.
   *
   * @param item Element
   * @return {@code true}, wenn vorhanden
   */
  public boolean contains(T item) {
    return indexOf(item) >= 0;
  }

  /** {@code Clear}. */
  public void clear() {
    Arrays.fill(items, 0, size, null);
    size = 0;
    version++;
  }

  /** Aufzählung wie {@code List<T>.Enumerator}: wirft bei Änderung während der Aufzählung. */
  @Override
  public Iterator<T> iterator() {
    return new Iterator<>() {
      private final int expectedVersion = version;
      private int index;

      @Override
      public boolean hasNext() {
        Enumeration.checkVersion(expectedVersion, version);
        return index < size;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        T item = get(index);
        index++;
        return item;
      }
    };
  }

  private void checkIndex(int index) {
    if (index < 0 || index >= size) {
      throw new ArgumentOutOfRangeException(
          "index",
          "Index was out of range. Must be non-negative and less than the size of the collection.");
    }
  }

  private void ensureCapacity(int needed) {
    if (needed > items.length) {
      items = Arrays.copyOf(items, java.lang.Math.max(needed, items.length * 2));
    }
  }
}
