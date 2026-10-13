package io.github.localsoul.port.engine.core.transform;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.component.Transform;
import io.github.localsoul.port.engine.api.component.WorldTransform;
import io.github.localsoul.port.engine.api.math.Quat;
import io.github.localsoul.port.engine.api.math.Vec3;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.World;
import io.github.localsoul.port.engine.core.CoreEngine;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransformHierarchyTest {

  private static final float HALF_PI = (float) (Math.PI / 2.0);

  private static Vec3 worldPosition(World world, Entity entity) {
    return world.worldMatrix(entity).translation();
  }

  @Test
  void movingParentMovesChildInWorldCoordinates() {
    CoreEngine engine = CoreEngine.builder().build();
    World world = engine.world();
    Entity parent = world.create();
    Entity child = world.create();
    world.add(parent, Transform.at(new Vec3(10f, 0f, 0f)));
    world.add(child, Transform.at(new Vec3(1f, 2f, 0f)));
    world.setParent(child, parent);
    assertEquals(new Vec3(11f, 2f, 0f), worldPosition(world, child));

    world.set(parent, Transform.at(new Vec3(-5f, 1f, 3f)));
    assertEquals(new Vec3(-4f, 3f, 3f), worldPosition(world, child));
    assertEquals(new Vec3(1f, 2f, 0f), world.get(child, Transform.class).position());
  }

  @Test
  void rotatingParentRotatesChildAroundParentOrigin() {
    World world = CoreEngine.builder().build().world();
    Entity parent = world.create();
    Entity child = world.create();
    world.add(parent, Transform.at(new Vec3(5f, 0f, 0f)));
    world.add(child, Transform.at(new Vec3(2f, 0f, 0f)));
    world.setParent(child, parent);
    world.set(
        parent,
        world.get(parent, Transform.class).withRotation(Quat.fromAxisAngle(Vec3.UNIT_Y, HALF_PI)));
    // Kind liegt 2 m auf +X des Elternobjekts; nach 90° um Y zeigt +X nach -Z
    assertTrue(
        worldPosition(world, child).approxEquals(new Vec3(5f, 0f, -2f)),
        worldPosition(world, child).toString());
    assertTrue(worldPosition(world, parent).approxEquals(new Vec3(5f, 0f, 0f)));
  }

  @Test
  void parentScaleScalesChildOffset() {
    World world = CoreEngine.builder().build().world();
    Entity parent = world.create();
    Entity child = world.create();
    world.add(parent, new Transform(Vec3.ZERO, Quat.IDENTITY, new Vec3(2f, 2f, 2f)));
    world.add(child, Transform.at(new Vec3(1f, 1f, 0f)));
    world.setParent(child, parent);
    assertEquals(new Vec3(2f, 2f, 0f), worldPosition(world, child));
  }

  @Test
  void entitiesWithoutTransformCountAsIdentityInChain() {
    World world = CoreEngine.builder().build().world();
    Entity group = world.create();
    Entity child = world.create();
    world.add(child, Transform.at(new Vec3(1f, 0f, 0f)));
    world.setParent(child, group);
    assertEquals(new Vec3(1f, 0f, 0f), worldPosition(world, child));
  }

  @Test
  void worldTransformIsComputedAtStartOfLate() {
    CoreEngine engine = CoreEngine.builder().build();
    World world = engine.world();
    Entity parent = world.create();
    Entity child = world.create();
    world.add(parent, Transform.at(new Vec3(1f, 0f, 0f)));
    world.add(child, Transform.at(new Vec3(0f, 1f, 0f)));
    world.setParent(child, parent);
    List<String> log = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx -> {
          log.add("UPDATE has=" + ctx.world().has(child, WorldTransform.class));
          ctx.world().set(parent, Transform.at(new Vec3(3f, 0f, 0f)));
        });
    engine.register(
        Phase.LATE,
        ctx ->
            log.add("LATE " + ctx.world().get(child, WorldTransform.class).matrix().translation()));
    engine.step(2);
    assertEquals(
        List.of(
            "UPDATE has=false",
            "LATE Vec3[x=3.0, y=1.0, z=0.0]",
            "UPDATE has=true",
            "LATE Vec3[x=3.0, y=1.0, z=0.0]"),
        log);
  }

  @Test
  void removesWorldTransformWhenTransformIsGone() {
    CoreEngine engine = CoreEngine.builder().build();
    World world = engine.world();
    Entity e = world.create();
    world.add(e, Transform.IDENTITY);
    engine.step(1);
    assertTrue(world.has(e, WorldTransform.class));
    world.remove(e, Transform.class);
    engine.step(1);
    assertFalse(world.has(e, WorldTransform.class));
  }
}
