package gen.fixtures;

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
    throw new UnsupportedOperationException(
        "PORT-TODO: Holder::Level:set: externer Typ UnityEngine.Mathf ohne Mapping");
  }

  @Override
  protected void awake() {
    Holder.Instances++;
  }
}
