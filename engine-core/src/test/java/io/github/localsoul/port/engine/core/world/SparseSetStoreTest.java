package io.github.localsoul.port.engine.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.world.Entity;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SparseSetStoreTest {

  private static List<Integer> indices(SparseSetStore<String> store) {
    List<Integer> result = new ArrayList<>();
    for (int i = 0; i < store.size(); i++) {
      result.add(store.entityAt(i).index());
    }
    return result;
  }

  @Test
  void iteratesInAscendingEntityOrderRegardlessOfInsertionOrder() {
    SparseSetStore<String> store = new SparseSetStore<>(String.class);
    int[] order = {7, 2, 40, 0, 5, 3};
    for (int index : order) {
      store.put(Entity.of(index, 0), "e" + index);
    }
    assertEquals(List.of(0, 2, 3, 5, 7, 40), indices(store));
    assertEquals("e5", store.componentAt(3));
  }

  @Test
  void removeKeepsOrderAndLookups() {
    SparseSetStore<String> store = new SparseSetStore<>(String.class);
    for (int i = 0; i < 6; i++) {
      store.put(Entity.of(i, 0), "e" + i);
    }
    assertTrue(store.remove(Entity.of(2, 0)));
    assertFalse(store.remove(Entity.of(2, 0)));
    assertEquals(List.of(0, 1, 3, 4, 5), indices(store));
    assertEquals("e4", store.getOrNull(Entity.of(4, 0)));
    assertNull(store.getOrNull(Entity.of(2, 0)));
    assertNull(store.getOrNull(Entity.of(999, 0)));
  }

  @Test
  void putReplacesExistingValue() {
    SparseSetStore<String> store = new SparseSetStore<>(String.class);
    store.put(Entity.of(1, 0), "a");
    store.put(Entity.of(1, 0), "b");
    assertEquals(1, store.size());
    assertEquals("b", store.getOrNull(Entity.of(1, 0)));
  }

  @Test
  void growsBeyondInitialCapacity() {
    SparseSetStore<String> store = new SparseSetStore<>(String.class);
    for (int i = 499; i >= 0; i--) {
      store.put(Entity.of(i * 3, 0), "x");
    }
    assertEquals(500, store.size());
    assertEquals(0, store.entityAt(0).index());
    assertEquals(1497, store.entityAt(499).index());
    assertTrue(store.contains(Entity.of(300, 0)));
  }

  @Test
  void rejectsInvalidPositionAndWrongType() {
    SparseSetStore<String> store = new SparseSetStore<>(String.class);
    assertThrows(IndexOutOfBoundsException.class, () -> store.entityAt(0));
    assertThrows(IndexOutOfBoundsException.class, () -> store.componentAt(-1));
    assertThrows(ClassCastException.class, () -> store.putObject(Entity.of(0, 0), 5));
  }
}
