package io.github.localsoul.port.engine.api.world;

/**
 * Lesesicht auf alle Komponenten eines Typs, für explizite Iteration mit einer {@code
 * for}-Schleife. Die Reihenfolge ist aufsteigend nach Entity-Index (deterministisch). Während einer
 * Phase ändert sich die Menge nicht (Strukturänderungen wirken am Phasenende); Werte können per
 * {@link World#set} ersetzt werden.
 *
 * @param <T> Komponententyp
 */
public interface ComponentStore<T> {

  /**
   * Liefert den Komponententyp.
   *
   * @return Komponententyp
   */
  Class<T> type();

  /**
   * Liefert die Anzahl der Komponenten.
   *
   * @return Anzahl
   */
  int size();

  /**
   * Liefert die Entity an einer Position.
   *
   * @param denseIndex Position 0 bis {@code size() - 1}
   * @return Entity
   * @throws IndexOutOfBoundsException bei ungültiger Position
   */
  Entity entityAt(int denseIndex);

  /**
   * Liefert die Komponente an einer Position.
   *
   * @param denseIndex Position 0 bis {@code size() - 1}
   * @return Komponente
   * @throws IndexOutOfBoundsException bei ungültiger Position
   */
  T componentAt(int denseIndex);
}
