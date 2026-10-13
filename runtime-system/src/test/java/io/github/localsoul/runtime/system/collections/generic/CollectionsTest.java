package io.github.localsoul.runtime.system.collections.generic;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.runtime.system.ArgumentException;
import io.github.localsoul.runtime.system.ArgumentNullException;
import io.github.localsoul.runtime.system.ArgumentOutOfRangeException;
import io.github.localsoul.runtime.system.InvalidOperationException;
import java.util.ArrayList;
import org.junit.jupiter.api.Test;

/** Collections mit .NET-Semantik (Reihenfolge, Ausnahmen, Änderung während der Aufzählung). */
class CollectionsTest {

  private static <T> java.util.List<T> listOf(Iterable<T> items) {
    java.util.List<T> result = new ArrayList<>();
    for (T item : items) {
      result.add(item);
    }
    return result;
  }

  @Test
  void listBasics() {
    List<Integer> list = new List<>();
    list.add(1);
    list.add(2);
    list.add(3);
    list.insert(0, 0);
    list.removeAt(2);
    assertEquals(java.util.List.of(0, 1, 3), listOf(list));
    assertEquals(3, list.count());
    assertTrue(list.remove(3));
    assertFalse(list.remove(42));
    list.set(0, 9);
    assertEquals(9, list.get(0));
    assertEquals(1, list.indexOf(1));
    list.clear();
    assertEquals(0, list.count());
  }

  @Test
  void listIndexOutOfRangeThrowsArgumentOutOfRange() {
    List<String> list = new List<>();
    list.add("a");
    assertThrows(ArgumentOutOfRangeException.class, () -> list.get(1));
    assertThrows(ArgumentOutOfRangeException.class, () -> list.get(-1));
    assertThrows(ArgumentOutOfRangeException.class, () -> list.removeAt(1));
    assertThrows(ArgumentOutOfRangeException.class, () -> list.insert(2, "x"));
    assertThrows(ArgumentOutOfRangeException.class, () -> new List<String>(-1));
  }

  @Test
  void modifyingListDuringForeachThrows() {
    List<Integer> list = new List<>();
    list.add(1);
    list.add(2);
    assertThrows(
        InvalidOperationException.class,
        () -> {
          for (Integer item : list) {
            list.add(item);
          }
        });
  }

  @Test
  void listEqualityTreatsSignedZerosAsEqual() {
    List<Float> list = new List<>();
    list.add(0.0f);
    assertTrue(list.contains(-0.0f));
  }

  /**
   * .NET liefert nach {@code Remove} und erneutem {@code Add} den neuen Eintrag an der frei
   * gewordenen Stelle (Referenzimplementierung von Dictionary, beobachtetes Verhalten; die
   * Dokumentation sichert keine Reihenfolge zu).
   */
  @Test
  void dictionaryOrderMatchesDotNetAfterRemoveAndAdd() {
    Dictionary<String, Integer> dict = new Dictionary<>();
    dict.add("a", 1);
    dict.add("b", 2);
    dict.add("c", 3);
    dict.remove("b");
    dict.add("d", 4);
    assertEquals(java.util.List.of("a", "d", "c"), listOf(dict.keys()));
    dict.remove("a");
    dict.remove("c");
    dict.add("e", 5);
    dict.add("f", 6);
    assertEquals(java.util.List.of("f", "d", "e"), listOf(dict.keys()));
    assertEquals(java.util.List.of(6, 4, 5), listOf(dict.values()));
  }

  @Test
  void dictionaryErrorsFollowDotNet() {
    Dictionary<String, Integer> dict = new Dictionary<>();
    dict.add("a", 1);
    assertThrows(ArgumentException.class, () -> dict.add("a", 2));
    assertThrows(KeyNotFoundException.class, () -> dict.get("zz"));
    assertThrows(ArgumentNullException.class, () -> dict.get(null));
    dict.set("a", 7);
    dict.set("b", 8);
    assertEquals(7, dict.get("a"));
    assertEquals(2, dict.count());
    assertTrue(dict.containsKey("b"));
  }

  @Test
  void dictionaryOverwriteDuringForeachThrowsLikeMono() {
    Dictionary<String, Integer> dict = new Dictionary<>();
    dict.add("a", 1);
    dict.add("b", 2);
    assertThrows(
        InvalidOperationException.class,
        () -> {
          for (KeyValuePair<String, Integer> pair : dict) {
            dict.set(pair.key(), 0);
          }
        });
  }

  @Test
  void dictionaryKeysTreatSignedZerosAsEqual() {
    Dictionary<Float, String> dict = new Dictionary<>();
    dict.add(0.0f, "null");
    assertEquals("null", dict.get(-0.0f));
    assertEquals("[0, null]", dict.iterator().next().toString());
  }

  @Test
  void hashSetFollowsDictionaryOrderAndAllowsNull() {
    HashSet<String> set = new HashSet<>();
    assertTrue(set.add("x"));
    assertFalse(set.add("x"));
    assertTrue(set.add(null));
    set.add("y");
    set.remove("x");
    set.add("z");
    assertEquals(java.util.Arrays.asList("z", null, "y"), listOf(set));
    assertTrue(set.contains(null));
  }

  @Test
  void queueAndStackOrderAndEmptyErrors() {
    Queue<Integer> queue = new Queue<>();
    queue.enqueue(1);
    queue.enqueue(2);
    assertEquals(java.util.List.of(1, 2), listOf(queue));
    assertEquals(1, queue.dequeue());
    assertEquals(2, queue.peek());
    queue.dequeue();
    assertThrows(InvalidOperationException.class, queue::dequeue);
    Stack<Integer> stack = new Stack<>();
    stack.push(1);
    stack.push(2);
    assertEquals(java.util.List.of(2, 1), listOf(stack));
    assertEquals(2, stack.pop());
    stack.pop();
    assertThrows(InvalidOperationException.class, stack::peek);
  }
}
