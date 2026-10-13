package gen.fixtures;

/** Generiert aus dem IR-Typ {@code UnsafeBits}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class UnsafeBitsBase {
  public abstract int first(int[] values);

  public int plain(int a) {
    return a + 1;
  }
}
