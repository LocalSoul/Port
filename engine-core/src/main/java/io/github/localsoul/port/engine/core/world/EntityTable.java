package io.github.localsoul.port.engine.core.world;

import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.StaleEntityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Vergibt Entity-Handles. Freie Indizes werden in FIFO-Reihenfolge wiederverwendet
 * (deterministisch); beim Zerstören steigt die Generation des Index.
 */
final class EntityTable {

  private int[] generations = new int[16];
  private boolean[] alive = new boolean[16];
  private int nextIndex;
  private final ArrayDeque<Integer> freeIndices = new ArrayDeque<>();

  Entity create() {
    int index;
    if (freeIndices.isEmpty()) {
      index = nextIndex;
      nextIndex++;
      ensureCapacity(index + 1);
    } else {
      index = freeIndices.removeFirst();
    }
    alive[index] = true;
    return Entity.of(index, generations[index]);
  }

  void destroy(Entity entity) {
    int index = entity.index();
    alive[index] = false;
    generations[index]++;
    freeIndices.addLast(index);
  }

  boolean isAlive(Entity entity) {
    int index = entity.index();
    if (index < 0 || index >= nextIndex) {
      return false;
    }
    return alive[index] && generations[index] == entity.generation();
  }

  /** Wirft {@link StaleEntityException}, wenn die Entity nicht lebt. */
  void check(Entity entity) {
    if (!isAlive(entity)) {
      throw new StaleEntityException(describeStale(entity));
    }
  }

  private String describeStale(Entity entity) {
    int index = entity.index();
    if (index < 0 || index >= nextIndex) {
      return "Entity " + entity + " existiert nicht";
    }
    if (!alive[index]) {
      return "Entity " + entity + " ist zerstört";
    }
    return "Entity " + entity + " ist veraltet, aktuelle Generation ist " + generations[index];
  }

  /** Lebende Entities aufsteigend nach Index. */
  List<Entity> aliveEntities() {
    List<Entity> result = new ArrayList<>();
    for (int index = 0; index < nextIndex; index++) {
      if (alive[index]) {
        result.add(Entity.of(index, generations[index]));
      }
    }
    return result;
  }

  private void ensureCapacity(int size) {
    if (size <= generations.length) {
      return;
    }
    int newLength = Math.max(size, generations.length * 2);
    generations = Arrays.copyOf(generations, newLength);
    alive = Arrays.copyOf(alive, newLength);
  }
}
