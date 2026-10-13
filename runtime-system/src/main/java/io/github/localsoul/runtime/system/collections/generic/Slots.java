package io.github.localsoul.runtime.system.collections.generic;

import io.github.localsoul.runtime.system.Equality;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Speicher von {@link Dictionary} und {@link HashSet} nach dem Muster von .NET: Einträge liegen in
 * einem Array in Einfügereihenfolge; entfernte Plätze kommen auf eine LIFO-Freiliste und werden vom
 * nächsten Einfügen zuerst belegt. Die Aufzählung läuft über das Array und überspringt freie
 * Plätze. Dadurch ist die Reihenfolge identisch zu .NET und unabhängig von Hashwerten (ADR-019).
 * Gesucht wird über eine Java-Map (Schlüssel normalisiert nach {@link Equality#key}); sie wird nie
 * iteriert.
 *
 * @param <K> Schlüsseltyp
 * @param <V> Werttyp
 */
final class Slots<K, V> {

  private Object[] keys = new Object[4];
  private Object[] values = new Object[4];
  private boolean[] used = new boolean[4];
  private int[] nextFree = new int[4];
  private int count;
  private int freeList = -1;
  private int freeCount;
  private int version;
  private final Map<Object, Integer> index = new HashMap<>();

  int size() {
    return count - freeCount;
  }

  int version() {
    return version;
  }

  /** Belegte Plätze liegen in {@code 0..end()-1}. */
  int end() {
    return count;
  }

  boolean isUsed(int slot) {
    return used[slot];
  }

  @SuppressWarnings("unchecked") // nur K wird gespeichert
  K keyAt(int slot) {
    return (K) keys[slot];
  }

  @SuppressWarnings("unchecked") // nur V wird gespeichert
  V valueAt(int slot) {
    return (V) values[slot];
  }

  /** Platz des Schlüssels oder −1. */
  int find(Object key) {
    Integer slot = index.get(indexKey(key));
    return slot == null ? -1 : slot;
  }

  /** Neuer Eintrag; belegt zuerst den zuletzt frei gewordenen Platz. */
  void insert(K key, V value) {
    int slot;
    if (freeCount > 0) {
      slot = freeList;
      freeList = nextFree[slot];
      freeCount--;
    } else {
      ensureCapacity(count + 1);
      slot = count;
      count++;
    }
    keys[slot] = key;
    values[slot] = value;
    used[slot] = true;
    index.put(indexKey(key), slot);
    version++;
  }

  /** Ersetzt den Wert; zählt wie in .NET Framework/Mono als Änderung. */
  void replace(int slot, V value) {
    values[slot] = value;
    version++;
  }

  boolean remove(Object key) {
    Integer slot = index.remove(indexKey(key));
    if (slot == null) {
      return false;
    }
    keys[slot] = null;
    values[slot] = null;
    used[slot] = false;
    nextFree[slot] = freeList;
    freeList = slot;
    freeCount++;
    version++;
    return true;
  }

  void clear() {
    if (count == 0) {
      return;
    }
    Arrays.fill(keys, 0, count, null);
    Arrays.fill(values, 0, count, null);
    Arrays.fill(used, 0, count, false);
    index.clear();
    count = 0;
    freeList = -1;
    freeCount = 0;
    version++;
  }

  /** Schlüssel der Suchtabelle: {@code null} wird zu einem Platzhalter, Zahlen normalisiert. */
  private static Object indexKey(Object key) {
    return key == null ? NullKey.INSTANCE : Equality.key(key);
  }

  /** Stellvertreter für {@code null} (HashSet erlaubt {@code null} wie .NET). */
  private enum NullKey {
    INSTANCE
  }

  private void ensureCapacity(int needed) {
    if (needed <= keys.length) {
      return;
    }
    int length = java.lang.Math.max(needed, keys.length * 2);
    keys = Arrays.copyOf(keys, length);
    values = Arrays.copyOf(values, length);
    used = Arrays.copyOf(used, length);
    nextFree = Arrays.copyOf(nextFree, length);
  }
}
