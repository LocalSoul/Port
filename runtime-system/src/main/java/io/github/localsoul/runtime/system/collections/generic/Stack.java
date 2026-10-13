package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.InvalidOperationException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * {@code System.Collections.Generic.Stack<T>}: Aufzählung vom obersten zum untersten Element,
 * leerer Stack wirft bei {@code Pop}/{@code Peek} {@link InvalidOperationException}.
 *
 * @param <T> Elementtyp
 */
public final class Stack<T> implements Iterable<T> {

  private final ArrayList<T> items = new ArrayList<>();
  private int version;

  /** {@code new Stack<T>()}. */
  public Stack() {}

  /**
   * {@code Count}.
   *
   * @return Anzahl
   */
  public int count() {
    return items.size();
  }

  /**
   * {@code Push}.
   *
   * @param item Element
   */
  public void push(T item) {
    items.add(item);
    version++;
  }

  /**
   * {@code Pop}.
   *
   * @return oberstes Element
   * @throws InvalidOperationException bei leerem Stack
   */
  public T pop() {
    T item = peek();
    items.remove(items.size() - 1);
    version++;
    return item;
  }

  /**
   * {@code Peek}.
   *
   * @return oberstes Element
   * @throws InvalidOperationException bei leerem Stack
   */
  public T peek() {
    if (items.isEmpty()) {
      throw new InvalidOperationException("Stack empty.");
    }
    return items.get(items.size() - 1);
  }

  /** {@code Clear}. */
  public void clear() {
    items.clear();
    version++;
  }

  @Override
  public Iterator<T> iterator() {
    int expected = version;
    return new Iterator<>() {
      private int position = items.size() - 1;

      @Override
      public boolean hasNext() {
        Enumeration.checkVersion(expected, version);
        return position >= 0;
      }

      @Override
      public T next() {
        if (!hasNext()) {
          throw new NoSuchElementException();
        }
        T item = items.get(position);
        position--;
        return item;
      }
    };
  }
}
