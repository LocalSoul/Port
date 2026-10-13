package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.InvalidOperationException;
import java.util.ArrayList;
import java.util.Iterator;

/**
 * {@code System.Collections.Generic.Queue<T>}: Aufzählung vom Anfang zum Ende, leere Queue wirft
 * bei {@code Dequeue}/{@code Peek} {@link InvalidOperationException}.
 *
 * @param <T> Elementtyp
 */
public final class Queue<T> implements Iterable<T> {

  private final ArrayList<T> items = new ArrayList<>();
  private int version;

  /** {@code new Queue<T>()}. */
  public Queue() {}

  /**
   * {@code Count}.
   *
   * @return Anzahl
   */
  public int count() {
    return items.size();
  }

  /**
   * {@code Enqueue}.
   *
   * @param item Element
   */
  public void enqueue(T item) {
    items.add(item);
    version++;
  }

  /**
   * {@code Dequeue}.
   *
   * @return erstes Element
   * @throws InvalidOperationException bei leerer Queue
   */
  public T dequeue() {
    T item = peek();
    items.remove(0);
    version++;
    return item;
  }

  /**
   * {@code Peek}.
   *
   * @return erstes Element
   * @throws InvalidOperationException bei leerer Queue
   */
  public T peek() {
    if (items.isEmpty()) {
      throw new InvalidOperationException("Queue empty.");
    }
    return items.get(0);
  }

  /** {@code Clear}. */
  public void clear() {
    items.clear();
    version++;
  }

  @Override
  public Iterator<T> iterator() {
    int expected = version;
    Iterator<T> inner = items.iterator();
    return new Iterator<>() {
      @Override
      public boolean hasNext() {
        Enumeration.checkVersion(expected, version);
        return inner.hasNext();
      }

      @Override
      public T next() {
        Enumeration.checkVersion(expected, version);
        return inner.next();
      }
    };
  }
}
