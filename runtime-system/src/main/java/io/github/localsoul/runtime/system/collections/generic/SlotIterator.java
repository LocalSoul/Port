package io.github.localsoul.runtime.system.collections.generic;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.function.IntFunction;

/** Aufzählung über belegte Plätze in Array-Reihenfolge, mit Versionsprüfung wie .NET. */
final class SlotIterator<E> implements Iterator<E> {

  private final Slots<?, ?> slots;
  private final IntFunction<E> element;
  private final int expectedVersion;
  private int position;

  SlotIterator(Slots<?, ?> slots, IntFunction<E> element) {
    this.slots = slots;
    this.element = element;
    this.expectedVersion = slots.version();
  }

  @Override
  public boolean hasNext() {
    Enumeration.checkVersion(expectedVersion, slots.version());
    while (position < slots.end() && !slots.isUsed(position)) {
      position++;
    }
    return position < slots.end();
  }

  @Override
  public E next() {
    if (!hasNext()) {
      throw new NoSuchElementException();
    }
    E result = element.apply(position);
    position++;
    return result;
  }
}
