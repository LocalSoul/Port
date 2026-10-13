package gen.fixtures;

/** Generiert aus dem IR-Typ {@code Box}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class BoxBase<T> {
  T value;

  public T get() {
    return this.value;
  }

  public void put(T item) {
    this.value = item;
  }
}
