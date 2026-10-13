package io.github.localsoul.port.engine.api.world;

/**
 * Handle einer Entity: 64-Bit-ID aus Index (untere 32 Bit) und Generation (obere 32 Bit). Wird eine
 * Entity zerstört, steigt die Generation ihres Index; alte Handles werden dadurch als veraltet
 * erkannt ({@link StaleEntityException}).
 *
 * @param id kombinierte ID
 */
public record Entity(long id) {

  /**
   * Erzeugt ein Handle aus Index und Generation.
   *
   * @param index Index, nicht negativ
   * @param generation Generation, nicht negativ
   * @return das Handle
   * @throws IllegalArgumentException bei negativen Werten
   */
  public static Entity of(int index, int generation) {
    if (index < 0 || generation < 0) {
      throw new IllegalArgumentException("Index und Generation dürfen nicht negativ sein");
    }
    long id = ((long) generation << 32) | (index & 0xFFFFFFFFL);
    return new Entity(id);
  }

  /**
   * Liefert den Index (Platz in den Speichern).
   *
   * @return Index, untere 32 Bit
   */
  public int index() {
    return (int) id;
  }

  /**
   * Liefert die Generation.
   *
   * @return Generation, obere 32 Bit
   */
  public int generation() {
    return (int) (id >>> 32);
  }

  /** Liefert {@code index:generation}, z. B. {@code 3:1}. */
  @Override
  public String toString() {
    return index() + ":" + generation();
  }
}
