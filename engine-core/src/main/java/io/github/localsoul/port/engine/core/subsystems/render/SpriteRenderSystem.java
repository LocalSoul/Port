package io.github.localsoul.port.engine.core.subsystems.render;

import io.github.localsoul.port.engine.api.component.Camera2D;
import io.github.localsoul.port.engine.api.component.Schemas;
import io.github.localsoul.port.engine.api.component.Sprite;
import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.component.UiCanvas;
import io.github.localsoul.port.engine.api.component.Viewport;
import io.github.localsoul.port.engine.api.component.WorldTransform;
import io.github.localsoul.port.engine.api.input.KeyEvent;
import io.github.localsoul.port.engine.api.math.Mat4;
import io.github.localsoul.port.engine.api.render.Affine2D;
import io.github.localsoul.port.engine.api.render.RenderBackend;
import io.github.localsoul.port.engine.api.render.Renderer;
import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.subsystem.EventBus;
import io.github.localsoul.port.engine.api.subsystem.SubsystemInfo;
import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.World;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * 2D-Renderer (ADR-014): zeichnet in Phase {@code LATE} alle sichtbaren {@link Sprite}s durch die
 * erste {@link Camera2D} auf das {@link RenderBackend}, danach alle {@link UiCanvas}-Elemente.
 * Reicht Tastenereignisse des Backends als {@link KeyEvent} an den Event-Bus weiter und schreibt
 * die Größe der Zeichenfläche als {@link Viewport}.
 *
 * <p>Abbildung: {@code pixelsPerWorldUnit = höhe / (2 · orthographicSize)}, Bildschirm-y nach
 * unten. Reihenfolge: {@code order}, dann Entity-Index (deterministisch). Ohne Kamera gilt Größe 5
 * mit schwarzem Hintergrund.
 */
public final class SpriteRenderSystem implements Renderer {

  /** ID dieses Providers. */
  public static final String ID = "render-sprite2d";

  /** Größe ohne Kamera. */
  static final float DEFAULT_SIZE = 5f;

  private static final SubsystemInfo INFO =
      new SubsystemInfo(
          ID,
          "render",
          Set.of(
              Schemas.WORLD_TRANSFORM,
              Schemas.TEXTURE,
              Schemas.SPRITE,
              Schemas.CAMERA_2D,
              Schemas.UI_CANVAS),
          Set.of(Schemas.VIEWPORT),
          Set.of(KeyEvent.TYPE),
          Set.of(),
          Set.of(),
          Set.of());

  private final RenderBackend backend;
  private World world;
  private EventBus bus;
  private Entity viewport;
  private long framesRendered;

  /**
   * Erzeugt den Renderer.
   *
   * @param backend Zeichenfläche
   */
  public SpriteRenderSystem(RenderBackend backend) {
    this.backend = backend;
  }

  @Override
  public SubsystemInfo info() {
    return INFO;
  }

  @Override
  public void init(World world, EventBus bus) {
    this.world = world;
    this.bus = bus;
    for (Class<?> type :
        List.of(Texture.class, Sprite.class, Camera2D.class, UiCanvas.class, Viewport.class)) {
      world.registerComponent(type);
    }
    viewport = world.create();
    world.add(viewport, new Viewport(backend.width(), backend.height()));
  }

  @Override
  public void run(FrameContext context) {
    if (context.phase() != Phase.LATE) {
      return;
    }
    for (KeyEvent key : backend.pollKeys()) {
      bus.publish(KeyEvent.TYPE, key);
    }
    Viewport size = new Viewport(backend.width(), backend.height());
    if (!size.equals(world.get(viewport, Viewport.class))) {
      world.set(viewport, size);
    }
    Camera camera = camera();
    backend.begin(camera.background);
    for (Entity entity : sprites()) {
      draw(entity, world.get(entity, Sprite.class), camera);
    }
    drawUi();
    backend.present();
    framesRendered++;
  }

  private record Camera(float x, float y, float size, int background) {}

  private Camera camera() {
    ComponentStore<Camera2D> cameras = world.store(Camera2D.class);
    for (int i = 0; i < cameras.size(); i++) {
      Entity entity = cameras.entityAt(i);
      Camera2D camera = cameras.componentAt(i);
      if (world.has(entity, WorldTransform.class)) {
        Mat4 matrix = world.get(entity, WorldTransform.class).matrix();
        return new Camera(
            matrix.get(0, 3), matrix.get(1, 3), camera.orthographicSize(), camera.background());
      }
    }
    return new Camera(0f, 0f, DEFAULT_SIZE, 0xFF000000);
  }

  /** Sichtbare Sprites mit Position und vorhandener Textur, nach Reihenfolge und Index sortiert. */
  private List<Entity> sprites() {
    List<Entity> result = new ArrayList<>();
    ComponentStore<Sprite> store = world.store(Sprite.class);
    for (int i = 0; i < store.size(); i++) {
      Entity entity = store.entityAt(i);
      Sprite sprite = store.componentAt(i);
      boolean drawable =
          sprite.visible()
              && sprite.texture() != null
              && world.isAlive(sprite.texture())
              && world.has(sprite.texture(), Texture.class)
              && world.has(entity, WorldTransform.class);
      if (drawable) {
        result.add(entity);
      }
    }
    result.sort(
        (a, b) -> {
          int byOrder =
              Integer.compare(
                  world.get(a, Sprite.class).order(), world.get(b, Sprite.class).order());
          return byOrder != 0 ? byOrder : Integer.compare(a.index(), b.index());
        });
    return result;
  }

  private void draw(Entity entity, Sprite sprite, Camera camera) {
    Texture texture = world.get(sprite.texture(), Texture.class);
    Mat4 m = world.get(entity, WorldTransform.class).matrix();
    backend.drawTexture(texture, affine(m, sprite, texture, camera), sprite.tint());
  }

  /**
   * Texturpixel (u nach rechts, v nach unten) → lokale Welteinheiten (y nach oben, Drehpunkt im
   * Ursprung) → Welt (Matrix der Entity) → Bildschirm (Kamera).
   */
  Affine2D affine(Mat4 m, Sprite sprite, Texture texture, Camera camera) {
    double ppw = backend.height() / (2.0 * camera.size());
    double ppu = sprite.pixelsPerUnit();
    double offsetX = -sprite.pivotX() * texture.width() / ppu;
    double offsetY = texture.height() * (1.0 - sprite.pivotY()) / ppu;
    double m00 = m.get(0, 0);
    double m01 = m.get(0, 1);
    double m03 = m.get(0, 3);
    double m10 = m.get(1, 0);
    double m11 = m.get(1, 1);
    double m13 = m.get(1, 3);
    return new Affine2D(
        ppw * m00 / ppu,
        -ppw * m01 / ppu,
        ppw * (m00 * offsetX + m01 * offsetY + m03 - camera.x()) + backend.width() / 2.0,
        -ppw * m10 / ppu,
        ppw * m11 / ppu,
        backend.height() / 2.0 - ppw * (m10 * offsetX + m11 * offsetY + m13 - camera.y()));
  }

  private void drawUi() {
    ComponentStore<UiCanvas> canvases = world.store(UiCanvas.class);
    for (int i = 0; i < canvases.size(); i++) {
      for (UiCanvas.Element element : canvases.componentAt(i).elements()) {
        switch (element) {
          case UiCanvas.Box box ->
              backend.fillRect(
                  box.x(), box.y(), box.width(), box.height(), box.fill(), box.border());
          case UiCanvas.Text text ->
              backend.drawText(
                  text.text(),
                  text.x(),
                  text.y(),
                  text.width(),
                  text.height(),
                  text.size(),
                  text.bold(),
                  text.anchor(),
                  text.color());
        }
      }
    }
  }

  @Override
  public long framesRendered() {
    return framesRendered;
  }

  @Override
  public void shutdown() {
    backend.close();
  }
}
