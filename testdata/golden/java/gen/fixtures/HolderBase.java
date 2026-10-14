package gen.fixtures;

import io.github.localsoul.adapter.unity.Mathf;
import io.github.localsoul.adapter.unity.MonoBehaviour;

/** Generiert aus dem IR-Typ {@code Holder}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class HolderBase extends MonoBehaviour {
  public static int Instances;

  float health = 100.0f;

  int level;

  public float health() {
    return this.health;
  }

  public int level() {
    return this.level;
  }

  public void level(int value) {
    this.level = Mathf.clamp(value, 0, 10);
  }

  @Override
  protected void awake() {
    Holder.Instances++;
  }
}
