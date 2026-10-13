package io.github.localsoul.port.engine.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.component.Renderable;
import io.github.localsoul.port.engine.api.component.Transform;
import io.github.localsoul.port.engine.api.math.Vec3;
import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.World;
import io.github.localsoul.port.engine.core.subsystems.input.InputScript;
import io.github.localsoul.port.engine.core.subsystems.input.ScriptedInput;
import io.github.localsoul.port.engine.core.subsystems.render.NullRenderer;
import org.junit.jupiter.api.Test;

/** Mini-Szene aus M1.5: eine Entity, deren Komponente per Update die Position ändert. */
class HeadlessSceneTest {

  private static final float FRAME = 1f / 60f;

  /** Geschwindigkeit in Meter pro Sekunde. */
  record Velocity(Vec3 value) {}

  /** UPDATE: bewegt alle Entities mit {@link Velocity}; mit {@code "Stop"} gehalten steht alles. */
  private static void move(FrameContext ctx) {
    if (ctx.input().isHeld("Stop")) {
      return;
    }
    World world = ctx.world();
    ComponentStore<Velocity> velocities = world.store(Velocity.class);
    for (int i = 0; i < velocities.size(); i++) {
      Entity e = velocities.entityAt(i);
      Transform t = world.get(e, Transform.class);
      Vec3 step = velocities.componentAt(i).value().scale(ctx.time().delta());
      world.set(e, t.withPosition(t.position().add(step)));
    }
  }

  private static Entity setUp(CoreEngine engine) {
    World world = engine.world();
    world.registerComponent(Velocity.class);
    Entity mover = world.create();
    world.add(mover, Transform.at(new Vec3(0f, 1f, 0f)));
    world.add(mover, new Velocity(new Vec3(3f, 0f, -1.5f)));
    world.add(mover, Renderable.of("cube"));
    engine.register(Phase.UPDATE, HeadlessSceneTest::move);
    return mover;
  }

  private static Vec3 expectedAfter(int frames) {
    Vec3 position = new Vec3(0f, 1f, 0f);
    Vec3 step = new Vec3(3f, 0f, -1.5f).scale(FRAME);
    for (int i = 0; i < frames; i++) {
      position = position.add(step);
    }
    return position;
  }

  @Test
  void miniSceneRuns100FramesToExpectedState() {
    CoreEngine engine = CoreEngine.builder().frameDelta(FRAME).build();
    Entity mover = setUp(engine);
    engine.step(100);
    Vec3 position = engine.world().get(mover, Transform.class).position();
    assertEquals(expectedAfter(100), position);
    assertTrue(position.approxEquals(new Vec3(5f, 1f, -2.5f), 1e-4f), position.toString());
    assertEquals(100L, engine.frameCount());
  }

  @Test
  void miniSceneRunsUnchangedOverSubsystems() {
    CoreEngine plain = CoreEngine.builder().frameDelta(FRAME).build();
    Entity plainMover = setUp(plain);
    plain.step(100);

    NullRenderer renderer = new NullRenderer();
    CoreEngine withSubsystems =
        CoreEngine.builder()
            .frameDelta(FRAME)
            .subsystem(new ScriptedInput(new InputScript()))
            .subsystem(renderer)
            .build();
    Entity mover = setUp(withSubsystems);
    withSubsystems.step(100);

    assertEquals(
        plain.world().get(plainMover, Transform.class),
        withSubsystems.world().get(mover, Transform.class));
    assertEquals(100L, renderer.framesRendered());
    assertEquals(1, renderer.lastVisibleCount());
  }

  @Test
  void twoRunsWithSameScriptGiveIdenticalDumps() {
    assertEquals(scriptedRun(), scriptedRun());
  }

  @Test
  void scriptedStopKeyPausesMovement() {
    String dump = scriptedRun();
    // 100 Frames, davon 30 (Frame 20 bis 49) angehalten: 70 Bewegungsschritte
    Vec3 expected = expectedAfter(70);
    assertTrue(dump.contains("position=" + expected), dump);
  }

  private static String scriptedRun() {
    InputScript script = new InputScript().at(20).press("Stop").at(50).release("Stop");
    CoreEngine engine =
        CoreEngine.builder()
            .frameDelta(FRAME)
            .subsystem(new ScriptedInput(script))
            .subsystem(new NullRenderer())
            .build();
    setUp(engine);
    engine.step(100);
    engine.shutdown();
    return engine.world().dump();
  }
}
