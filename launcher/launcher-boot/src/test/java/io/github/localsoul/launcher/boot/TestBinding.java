package io.github.localsoul.launcher.boot;

import io.github.localsoul.port.engine.api.Engine;
import io.github.localsoul.port.engine.api.boot.EngineBinding;
import io.github.localsoul.port.engine.api.component.UiCanvas;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.world.Entity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Test-Adapter „test“: protokolliert Einstellungen, Objekte, Frames, den Achsenwert und gehaltene
 * Tasten; zeichnet eine rote Fläche über die ganze Ausgabe; ESCAPE beendet.
 */
public final class TestBinding implements EngineBinding {

  static final List<String> LOG = new ArrayList<>();

  /** Für {@link java.util.ServiceLoader}. */
  public TestBinding() {}

  @Override
  public String adapter() {
    return "test";
  }

  /** Rot, deckend. */
  static final int RED = 0xFFFF0000;

  @Override
  public void configure(Map<String, String> settings) {
    LOG.clear();
    for (Map.Entry<String, String> entry : settings.entrySet()) {
      LOG.add("setting " + entry.getKey() + "=" + entry.getValue());
    }
  }

  @Override
  public void install(Engine engine) {
    engine.world().registerComponent(UiCanvas.class);
    Entity ui = engine.world().create();
    engine
        .world()
        .add(ui, new UiCanvas(List.of(new UiCanvas.Box(0f, 0f, 10000f, 10000f, RED, RED))));
    engine.register(
        Phase.UPDATE,
        context -> {
          String line =
              "frame " + context.time().frame() + " axis " + context.input().axis("Horizontal");
          if (context.input().isHeld("R")) {
            line = line + " R";
          }
          LOG.add(line);
          if (context.input().wasPressed("ESCAPE")) {
            engine.requestQuit();
          }
        });
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
