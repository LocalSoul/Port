package gen.fixtures;

import io.github.localsoul.runtime.system.Func0;
import io.github.localsoul.runtime.system.collections.generic.List;

/** Generiert aus dem IR-Typ {@code Inventory}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class InventoryBase {
  Func0<Integer> counter;

  List<Integer> items = new List<>();

  Box<String> label = new Box<>();

  public int count() {
    return this.counter.invoke();
  }

  public void fill(int count) {
    this.items.clear();
    for (int i = 0; i < count; i++) {
      this.items.add(i);
    }
    this.items.set(0, 42);
    this.items.removeAt(count - 1);
    this.label.put("voll");
  }

  public String name() {
    return this.label.get();
  }

  public void setCounter(Func0<Integer> source) {
    this.counter = source;
  }

  public int total() {
    int sum = 0;
    for (int i = 0; i < this.items.count(); i++) {
      sum += this.items.get(i);
    }
    return sum;
  }
}
