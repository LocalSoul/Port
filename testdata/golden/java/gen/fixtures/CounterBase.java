package gen.fixtures;

import io.github.localsoul.adapter.unity.MonoBehaviour;

/** Generiert aus dem IR-Typ {@code Counter}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class CounterBase extends MonoBehaviour {
  int __auto_Total;

  int count;

  public String label = "Zähler";

  public int total() {
    return this.__auto_Total;
  }

  public void total(int value) {
    this.__auto_Total = value;
  }

  public int twice(int value) {
    return value * 2;
  }

  @Override
  protected void update() {
    throw new UnsupportedOperationException(
        "PORT-TODO: Counter::Update#: externes Symbol UnityEngine.Input::GetKey#string ohne Mapping (static-method/operator)");
  }
}
