package io.github.localsoul.launcher.boot;

import io.github.localsoul.port.engine.api.Engine;
import io.github.localsoul.port.engine.api.boot.EngineBinding;
import io.github.localsoul.port.engine.api.schedule.Phase;
import java.util.ArrayList;
import java.util.List;

/** Test-Adapter „test“: protokolliert Objekte, Frames und den Achsenwert. */
public final class TestBinding implements EngineBinding {

  static final List<String> LOG = new ArrayList<>();

  /** Für {@link java.util.ServiceLoader}. */
  public TestBinding() {}

  @Override
  public String adapter() {
    return "test";
  }

  @Override
  public void install(Engine engine) {
    LOG.clear();
    engine.register(
        Phase.UPDATE,
        context ->
            LOG.add(
                "frame " + context.time().frame() + " axis " + context.input().axis("Horizontal")));
  }

  @Override
  public void spawn(String name, Object script) {
    if (!(script instanceof Script)) {
      throw new IllegalArgumentException(name);
    }
    LOG.add("spawn " + name);
  }

  /** Spielklasse für Tests. */
  public static class Script {
    /** Standardkonstruktor. */
    public Script() {}
  }

  /** Spielklasse, deren Konstruktor scheitert. */
  public static class Broken {
    /** Wirft immer. */
    public Broken() {
      throw new IllegalStateException("kaputt");
    }
  }
}
