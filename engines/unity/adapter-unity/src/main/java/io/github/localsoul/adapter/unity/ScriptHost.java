package io.github.localsoul.adapter.unity;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter-eigene Komponente (ADR-016): hält das {@link GameObject} und seine Skripte. Der Core
 * speichert sie nur, ohne ihren Inhalt zu kennen.
 */
final class ScriptHost {

  private final GameObject gameObject;
  private final List<MonoBehaviour> scripts = new ArrayList<>();

  ScriptHost(GameObject gameObject) {
    this.gameObject = gameObject;
  }

  GameObject gameObject() {
    return gameObject;
  }

  List<MonoBehaviour> scripts() {
    return scripts;
  }

  void add(MonoBehaviour script) {
    scripts.add(script);
  }

  /** Deterministisch für den Welt-Dump: Name und Klassennamen der Skripte. */
  @Override
  public String toString() {
    StringBuilder out = new StringBuilder("ScriptHost[").append(gameObject.name());
    for (MonoBehaviour script : scripts) {
      out.append(", ").append(script.getClass().getSimpleName());
    }
    return out.append(']').toString();
  }
}
