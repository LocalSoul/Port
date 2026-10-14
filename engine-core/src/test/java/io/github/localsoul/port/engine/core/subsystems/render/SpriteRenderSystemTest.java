package io.github.localsoul.port.engine.core.subsystems.render;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.component.Camera2D;
import io.github.localsoul.port.engine.api.component.InputState;
import io.github.localsoul.port.engine.api.component.Sprite;
import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.component.Transform;
import io.github.localsoul.port.engine.api.component.UiCanvas;
import io.github.localsoul.port.engine.api.component.Viewport;
import io.github.localsoul.port.engine.api.input.KeyEvent;
import io.github.localsoul.port.engine.api.math.Quat;
import io.github.localsoul.port.engine.api.math.Vec3;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.World;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystems.input.KeyboardInput;
import java.util.List;
import org.junit.jupiter.api.Test;

class SpriteRenderSystemTest {

  private final RecordingBackend backend = new RecordingBackend(200, 100);

  private CoreEngine engine() {
    return CoreEngine.builder()
        .subsystem(new KeyboardInput())
        .subsystem(new SpriteRenderSystem(backend))
        .build();
  }

  private static Entity texture(World world, int size) {
    Entity texture = world.create();
    world.add(texture, new Texture(size, size, new int[size * size], true));
    return texture;
  }

  private static Entity sprite(World world, Entity texture, float x, float y, int order) {
    Entity entity = world.create();
    world.add(entity, new Transform(new Vec3(x, y, 0f), Quat.IDENTITY, new Vec3(1f, 1f, 1f)));
    world.add(entity, new Sprite(texture, 0.5f, 0.5f, 2f, order, 0xFFFFFFFF, true));
    return entity;
  }

  @Test
  void viewportIsKnownBeforeTheFirstFrame() {
    CoreEngine engine = engine();
    Viewport viewport = engine.world().store(Viewport.class).componentAt(0);
    assertEquals(new Viewport(200, 100), viewport);
  }

  @Test
  void spritesAreMappedThroughTheCameraAndSortedByOrder() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity camera = world.create();
    world.add(camera, new Transform(Vec3.ZERO, Quat.IDENTITY, new Vec3(1f, 1f, 1f)));
    world.add(camera, new Camera2D(5f, 0xFF314D79));
    Entity small = texture(world, 2);
    sprite(world, small, 1f, 0f, 5);
    Entity big = texture(world, 4);
    sprite(world, big, 0f, 0f, 1);
    engine.step(1);
    // 100 px Höhe / (2 · 5) = 10 px je Einheit; 2 px je Einheit in der Textur → Faktor 5.
    // 2x2-Textur, Mitte im Ursprung: Pixel (0,0) bei (100 - 5, 50 - 5); um 1 Einheit nach rechts:
    // +10.
    assertEquals(
        List.of(
            "begin ff314d79",
            "texture 4x4 [5.00 0.00 90.00 | 0.00 5.00 40.00]",
            "texture 2x2 [5.00 0.00 105.00 | 0.00 5.00 45.00]",
            "present"),
        backend.log);
  }

  @Test
  void rotationAndUiAreDrawn() {
    CoreEngine engine = engine();
    World world = engine.world();
    Entity tex = texture(world, 2);
    Entity rotated = sprite(world, tex, 0f, 0f, 0);
    world.set(
        rotated,
        new Transform(
            Vec3.ZERO,
            Quat.fromAxisAngle(new Vec3(0f, 0f, 1f), (float) (Math.PI / 2)),
            new Vec3(1f, 1f, 1f)));
    Entity ui = world.create();
    world.add(
        ui,
        new UiCanvas(
            List.of(
                new UiCanvas.Box(10f, 10f, 50f, 20f, 0x80000000, 0),
                new UiCanvas.Text(10f, 10f, 50f, 20f, "Sterne: 3", 28, true, 0, 0xFFFFFFFF))));
    engine.step(1);
    // 90° gegen den Uhrzeigersinn in der Welt: Textur-x zeigt auf dem Bildschirm nach oben.
    assertTrue(backend.log.get(1).startsWith("texture 2x2 [0.00 5.00"), backend.log.toString());
    assertEquals("rect 10 10 50 20", backend.log.get(2));
    assertEquals("text Sterne: 3 size=28 bold=true anchor=0", backend.log.get(3));
  }

  @Test
  void keysBecomeInputStateInTheNextFrame() {
    CoreEngine engine = engine();
    backend.keys.add(new KeyEvent("LEFT", true));
    engine.step(1);
    engine.step(1);
    InputState state = engine.world().store(InputState.class).componentAt(0);
    assertEquals(List.of("LEFT"), List.copyOf(state.held()));
    assertEquals(List.of("LEFT"), List.copyOf(state.pressed()));
    engine.step(1);
    state = engine.world().store(InputState.class).componentAt(0);
    assertTrue(state.pressed().isEmpty());
    backend.keys.add(new KeyEvent("LEFT", false));
    engine.step(2);
    state = engine.world().store(InputState.class).componentAt(0);
    assertTrue(state.held().isEmpty());
  }

  @Test
  void variableFramesAndQuit() {
    CoreEngine engine = engine();
    engine.stepFrame(0.05f);
    engine.stepFrame(0.01f);
    assertEquals(2, engine.frameCount());
    assertFalse(engine.quitRequested());
    engine.requestQuit();
    assertTrue(engine.quitRequested());
    engine.shutdown();
    assertTrue(backend.log.contains("close"));
  }
}
