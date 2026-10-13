package io.github.localsoul.port.ir.atom;

/**
 * Array-Typ, z. B. {@code i32[]} (Rang 1) oder {@code f32[,]} (Rang 2).
 *
 * @param elementType Elementtyp
 * @param rank Rang, mindestens 1
 */
public record ArrayType(TypeRef elementType, int rank) implements TypeRef {

  /**
   * Prüft den Rang.
   *
   * @throws IllegalArgumentException wenn {@code rank} kleiner als 1 ist
   */
  public ArrayType {
    if (rank < 1) {
      throw new IllegalArgumentException("Rang muss mindestens 1 sein: " + rank);
    }
  }

  @Override
  public String toString() {
    StringBuilder out = new StringBuilder();
    out.append(elementType).append('[');
    for (int i = 1; i < rank; i++) {
      out.append(',');
    }
    out.append(']');
    return out.toString();
  }
}
