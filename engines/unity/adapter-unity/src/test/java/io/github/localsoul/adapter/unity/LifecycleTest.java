package io.github.localsoul.adapter.unity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.localsoul.port.engine.core.CoreEngine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Lifecycle gegen eine Referenzabfolge nach der Unity-Dokumentation („Execution Order of Event
 * Functions"): Awake → OnEnable sofort beim Anhängen, Start vor dem ersten Update, je Frame
 * FixedUpdate (0..n) → Update → LateUpdate.
 */
class LifecycleTest {

  /** Protokolliert jeden Aufruf mit Frame-Nummer. */
  static class Recorder extends MonoBehaviour {
    final String id;
    final List<String> log;

    Recorder(String id, List<String> log) {
      this.id = id;
      this.log = log;
    }

    private void record(String event) {
      var context = UnityRuntime.current();
      String frame = context == null ? "-" : Long.toString(context.time().frame());
      log.add(frame + " " + id + "." + event);
    }

    @Override
    protected void awake() {
      record("Awake");
    }

    @Override
    protected void onEnable() {
      record("OnEnable");
    }

    @Override
    protected void start() {
      record("Start");
    }

    @Override
    protected void fixedUpdate() {
      record("FixedUpdate");
    }

    @Override
    protected void update() {
      record("Update");
    }

    @Override
    protected void lateUpdate() {
      record("LateUpdate");
    }
  }

  /** Fügt in Frame 0 während Update ein zweites Skript hinzu. */
  static class Spawner extends Recorder {
    Spawner(List<String> log) {
      super("A", log);
    }

    @Override
    protected void update() {
      super.update();
      if (UnityRuntime.current().time().frame() == 0) {
        new GameObject("b").addComponent(new Recorder("B", log));
      }
    }
  }

  @Test
  void followsUnityExecutionOrder() {
    // Frame-Dauer 0,03 s bei Fixed-Delta 0,02 s: Frame 0 hat 1 FIXED, Frame 1 hat 2 (Akkumulator).
    CoreEngine engine = CoreEngine.builder().fixedDelta(0.02f).frameDelta(0.03f).build();
    UnityRuntime.install(engine);
    List<String> log = new ArrayList<>();
    new GameObject("a").addComponent(new Spawner(log));
    engine.step(2);
    List<String> expected =
        List.of(
            "- A.Awake",
            "- A.OnEnable",
            "0 A.Start",
            "0 A.FixedUpdate",
            "0 A.Update",
            "0 B.Awake",
            "0 B.OnEnable",
            "0 A.LateUpdate",
            "1 B.Start",
            "1 A.FixedUpdate",
            "1 B.FixedUpdate",
            "1 A.FixedUpdate",
            "1 B.FixedUpdate",
            "1 A.Update",
            "1 B.Update",
            "1 A.LateUpdate",
            "1 B.LateUpdate");
    assertEquals(expected, log);
  }

  @Test
  void deltaTimeIsFixedDeltaInsideFixedUpdate() {
    CoreEngine engine = CoreEngine.builder().fixedDelta(0.02f).frameDelta(0.05f).build();
    UnityRuntime.install(engine);
    List<String> log = new ArrayList<>();
    new GameObject("t")
        .addComponent(
            new MonoBehaviour() {
              @Override
              protected void fixedUpdate() {
                log.add("fixed " + Time.deltaTime());
              }

              @Override
              protected void update() {
                log.add("update " + Time.deltaTime() + " " + Time.fixedDeltaTime());
              }
            });
    engine.step(1);
    assertEquals(List.of("fixed 0.02", "fixed 0.02", "update 0.05 0.02"), log);
    assertEquals(0f, Time.deltaTime(), "außerhalb eines Frames");
  }
}
