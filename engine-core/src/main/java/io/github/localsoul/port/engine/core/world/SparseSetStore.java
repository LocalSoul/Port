package io.github.localsoul.port.engine.core.world;

import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.Entity;
import java.util.Arrays;

/**
 * Sparse-Set-Speicher für einen Komponententyp (ADR-016).
 *
 * <ul>
 *   <li>Dichte Arrays {@code entities}/{@code components}, <b>sortiert nach Entity-Index</b>, damit
 *       die Iteration deterministisch aufsteigend ist.
 *   <li>Index-Tabelle {@code sparse}: Entity-Index → Position im dichten Array plus 1 (0 = fehlt),
 *       Zugriff in O(1).
 *   <li>Einfügen und Entfernen verschieben das dichte Array per {@code arraycopy} (O(n)). Für M1
 *       genügt das; schneller wäre Tausch-Entfernen, das aber die Reihenfolge zerstört.
 * </ul>
 *
 * @param <T> Komponententyp
 */
public final class SparseSetStore<T> implements ComponentStore<T> {

  private final Class<T> type;
  private int[] sparse = new int[16];
  private Entity[] entities = new Entity[8];
  private Object[] components = new Object[8];
  private int size;

  /**
   * Erzeugt einen leeren Speicher.
   *
   * @param type Komponententyp
   */
  public SparseSetStore(Class<T> type) {
    this.type = type;
  }

  @Override
  public Class<T> type() {
    return type;
  }

  @Override
  public int size() {
    return size;
  }

  @Override
  public Entity entityAt(int denseIndex) {
    checkDenseIndex(denseIndex);
    return entities[denseIndex];
  }

  @Override
  public T componentAt(int denseIndex) {
    checkDenseIndex(denseIndex);
    return type.cast(components[denseIndex]);
  }

  /**
   * Prüft, ob der Entity-Index einen Eintrag hat (ohne Generationsprüfung, die macht die Welt).
   *
   * @param entity Entity
   * @return {@code true}, wenn vorhanden
   */
  public boolean contains(Entity entity) {
    return denseIndexOf(entity.index()) >= 0;
  }

  /**
   * Liefert die Komponente oder {@code null}.
   *
   * @param entity Entity
   * @return Komponente oder {@code null}
   */
  public T getOrNull(Entity entity) {
    int dense = denseIndexOf(entity.index());
    if (dense < 0) {
      return null;
    }
    return type.cast(components[dense]);
  }

  /**
   * Fügt ein oder ersetzt einen vorhandenen Wert.
   *
   * @param entity Entity
   * @param component Komponente
   */
  public void put(Entity entity, T component) {
    int index = entity.index();
    int dense = denseIndexOf(index);
    if (dense >= 0) {
      entities[dense] = entity;
      components[dense] = component;
      return;
    }
    insertSorted(entity, component);
  }

  /**
   * Wie {@link #put}, für Werte, deren Typ erst zur Laufzeit feststeht.
   *
   * @param entity Entity
   * @param component Komponente
   * @throws ClassCastException wenn {@code component} nicht vom Typ des Speichers ist
   */
  public void putObject(Entity entity, Object component) {
    put(entity, type.cast(component));
  }

  /**
   * Entfernt den Eintrag, falls vorhanden.
   *
   * @param entity Entity
   * @return {@code true}, wenn etwas entfernt wurde
   */
  public boolean remove(Entity entity) {
    int dense = denseIndexOf(entity.index());
    if (dense < 0) {
      return false;
    }
    sparse[entity.index()] = 0;
    int tail = size - dense - 1;
    System.arraycopy(entities, dense + 1, entities, dense, tail);
    System.arraycopy(components, dense + 1, components, dense, tail);
    size--;
    entities[size] = null;
    components[size] = null;
    reindexFrom(dense);
    return true;
  }

  private void insertSorted(Entity entity, T component) {
    ensureDenseCapacity(size + 1);
    ensureSparseCapacity(entity.index() + 1);
    int position = insertionPoint(entity.index());
    int tail = size - position;
    System.arraycopy(entities, position, entities, position + 1, tail);
    System.arraycopy(components, position, components, position + 1, tail);
    entities[position] = entity;
    components[position] = component;
    size++;
    reindexFrom(position);
  }

  /** Binäre Suche nach der ersten Position mit größerem Index. */
  private int insertionPoint(int index) {
    int low = 0;
    int high = size;
    while (low < high) {
      int middle = (low + high) >>> 1;
      if (entities[middle].index() < index) {
        low = middle + 1;
      } else {
        high = middle;
      }
    }
    return low;
  }

  private void reindexFrom(int dense) {
    for (int i = dense; i < size; i++) {
      sparse[entities[i].index()] = i + 1;
    }
  }

  private int denseIndexOf(int index) {
    if (index < 0 || index >= sparse.length) {
      return -1;
    }
    return sparse[index] - 1;
  }

  private void ensureDenseCapacity(int needed) {
    if (needed > entities.length) {
      int newLength = Math.max(needed, entities.length * 2);
      entities = Arrays.copyOf(entities, newLength);
      components = Arrays.copyOf(components, newLength);
    }
  }

  private void ensureSparseCapacity(int needed) {
    if (needed > sparse.length) {
      sparse = Arrays.copyOf(sparse, Math.max(needed, sparse.length * 2));
    }
  }

  private void checkDenseIndex(int denseIndex) {
    if (denseIndex < 0 || denseIndex >= size) {
      throw new IndexOutOfBoundsException("Position " + denseIndex + ", Größe " + size);
    }
  }
}
