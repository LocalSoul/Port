package io.github.localsoul.port.engine.core.world;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.component.Transform;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.DuplicateComponentException;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.MissingComponentException;
import io.github.localsoul.port.engine.api.world.World;
import io.github.localsoul.port.engine.core.CoreEngine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Strukturänderungen während eines Frames wirken am Phasenende (ADR-016). */
class DeferredChangesTest {

  record Tag(String name) {}

  private static CoreEngine engine() {
    CoreEngine engine = CoreEngine.builder().build();
    engine.world().registerComponent(Tag.class);
    return engine;
  }

  @Test
  void destroyDuringPhaseTakesEffectAtPhaseEnd() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity e = world.create();
    List<String> log = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          ctx.world().destroy(e);
          log.add("UPDATE nach destroy: " + ctx.world().isAlive(e));
        });
    engine.register(Phase.UPDATE, ctx -> log.add("UPDATE später: " + ctx.world().isAlive(e)));
    engine.register(Phase.LATE, ctx -> log.add("LATE: " + ctx.world().isAlive(e)));
    engine.step(1);
    assertEquals(List.of("UPDATE nach destroy: true", "UPDATE später: true", "LATE: false"), log);
  }

  @Test
  void addAndRemoveDuringIterationApplyAtPhaseEnd() {
    CoreEngine engine = engine();
    World world = engine.world();
    for (int i = 0; i < 5; i++) {
      Entity e = world.create();
      world.add(e, new Tag("t" + i));
    }
    List<Integer> sizesDuringIteration = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          ComponentStore<Tag> tags = ctx.world().store(Tag.class);
          for (int i = 0; i < tags.size(); i++) {
            Entity e = tags.entityAt(i);
            if (i % 2 == 0) {
              ctx.world().remove(e, Tag.class);
            }
            Entity spawned = ctx.world().create();
            ctx.world().add(spawned, new Tag("neu"));
            sizesDuringIteration.add(tags.size());
          }
        });
    List<Integer> sizesInLate = new ArrayList<>();
    engine.register(Phase.LATE, ctx -> sizesInLate.add(ctx.world().store(Tag.class).size()));
    assertDoesNotThrow(() -> engine.step(1));
    assertEquals(List.of(5, 5, 5, 5, 5), sizesDuringIteration);
    assertEquals(List.of(2 + 5), sizesInLate);
  }

  @Test
  void createAndSetActImmediately() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity e = world.create();
    world.add(e, new Tag("a"));
    List<String> log = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          Entity created = ctx.world().create();
          log.add("alive " + ctx.world().isAlive(created));
          ctx.world().set(e, new Tag("b"));
          log.add(ctx.world().get(e, Tag.class).name());
          ctx.world().add(created, new Tag("x"));
          log.add("has " + ctx.world().has(created, Tag.class));
        });
    engine.step(1);
    assertEquals(List.of("alive true", "b", "has false"), log);
  }

  @Test
  void validatesAgainstExpectedStateAtCallTime() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity e = world.create();
    world.add(e, new Tag("a"));
    List<Class<?>> errors = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          World w = ctx.world();
          w.remove(e, Tag.class);
          w.add(e, new Tag("ersetzt"));
          errors.add(catchType(() -> w.add(e, new Tag("doppelt"))));
          w.remove(e, Tag.class);
          errors.add(catchType(() -> w.remove(e, Tag.class)));
        });
    engine.step(1);
    assertEquals(
        List.of(DuplicateComponentException.class, MissingComponentException.class), errors);
    assertFalse(world.has(e, Tag.class));
  }

  private static Class<?> catchType(Runnable action) {
    try {
      action.run();
      return null;
    } catch (RuntimeException e) {
      return e.getClass();
    }
  }

  @Test
  void commandsForEntitiesDestroyedEarlierInThePhaseAreDropped() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity e = world.create();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          ctx.world().destroy(e);
          ctx.world().add(e, new Tag("zu spät"));
          ctx.world().destroy(e);
        });
    assertDoesNotThrow(() -> engine.step(1));
    assertFalse(world.isAlive(e));
  }

  @Test
  void destroyingParentDuringPhaseDestroysChildrenAtPhaseEnd() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity parent = world.create();
    Entity child = world.create();
    world.setParent(child, parent);
    List<Boolean> childAliveInUpdate = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          ctx.world().destroy(parent);
          childAliveInUpdate.add(ctx.world().isAlive(child));
        });
    engine.step(1);
    assertEquals(List.of(true), childAliveInUpdate);
    assertFalse(world.isAlive(parent));
    assertFalse(world.isAlive(child));
  }

  @Test
  void setParentDuringPhaseIsDeferred() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity parent = world.create();
    Entity child = world.create();
    List<Entity> seen = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          ctx.world().setParent(child, parent);
          seen.add(ctx.world().parentOf(child));
        });
    engine.register(Phase.LATE, ctx -> seen.add(ctx.world().parentOf(child)));
    engine.step(1);
    assertEquals(java.util.Arrays.asList(null, parent), seen);
  }

  @Test
  void changesInOneFixedStepAreVisibleInTheNext() {
    CoreEngine engine = CoreEngine.builder().fixedDelta(0.01f).frameDelta(0.02f).build();
    World world = engine.world();
    Entity e = world.create();
    List<Boolean> hasTransform = new ArrayList<>();
    engine.register(
        Phase.FIXED,
        ctx -> {
          hasTransform.add(ctx.world().has(e, Transform.class));
          if (!ctx.world().has(e, Transform.class)) {
            ctx.world().add(e, Transform.IDENTITY);
          }
        });
    engine.step(1);
    assertEquals(List.of(false, true), hasTransform);
  }

  @Test
  void outsideFramesChangesActImmediately() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity e = world.create();
    world.add(e, new Tag("a"));
    assertTrue(world.has(e, Tag.class));
    world.destroy(e);
    assertFalse(world.isAlive(e));
  }
}
