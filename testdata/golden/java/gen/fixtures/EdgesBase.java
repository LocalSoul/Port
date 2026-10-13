package gen.fixtures;

import io.github.localsoul.runtime.system.Func0;
import io.github.localsoul.runtime.system.MathF;

/** Generiert aus dem IR-Typ {@code Edges}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class EdgesBase {
  Func0<Integer> producer;

  public float angle(int i, int count) {
    return MathF.PI() / 2.0f + (float) i * MathF.PI() / (float) count;
  }

  public int produce() {
    return this.producer.invoke();
  }
}
