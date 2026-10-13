package io.github.localsoul.port.engine.core.subsystems.render;

import io.github.localsoul.port.engine.api.component.Renderable;
import io.github.localsoul.port.engine.api.component.Schemas;
import io.github.localsoul.port.engine.api.component.WorldTransform;
import io.github.localsoul.port.engine.api.render.Renderer;
import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.subsystem.EventBus;
import io.github.localsoul.port.engine.api.subsystem.SubsystemInfo;
import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.World;
import java.util.Set;

/**
 * Renderer ohne Ausgabe. Läuft in {@code LATE} (nach dem Transform-System) und zählt Frames sowie
 * sichtbare {@link Renderable}s mit {@link WorldTransform}. Kein Grafikcode, kein Fenster.
 */
public final class NullRenderer implements Renderer {

  /** ID dieses Providers. */
  public static final String ID = "render-null";

  private static final SubsystemInfo INFO =
      new SubsystemInfo(
          ID,
          "render",
          Set.of(Schemas.WORLD_TRANSFORM, Schemas.RENDERABLE),
          Set.of(),
          Set.of(),
          Set.of(),
          Set.of(),
          Set.of());

  private World world;
  private long framesRendered;
  private int lastVisibleCount;

  /** Erzeugt den Renderer. */
  public NullRenderer() {}

  @Override
  public SubsystemInfo info() {
    return INFO;
  }

  @Override
  public void init(World world, EventBus bus) {
    this.world = world;
  }

  @Override
  public void run(FrameContext context) {
    if (context.phase() != Phase.LATE) {
      return;
    }
    int visible = 0;
    ComponentStore<Renderable> renderables = world.store(Renderable.class);
    for (int i = 0; i < renderables.size(); i++) {
      boolean placed = world.has(renderables.entityAt(i), WorldTransform.class);
      if (renderables.componentAt(i).visible() && placed) {
        visible++;
      }
    }
    lastVisibleCount = visible;
    framesRendered++;
  }

  @Override
  public long framesRendered() {
    return framesRendered;
  }

  /**
   * Liefert die Anzahl sichtbarer Renderables im zuletzt gezeichneten Frame.
   *
   * @return Anzahl
   */
  public int lastVisibleCount() {
    return lastVisibleCount;
  }

  @Override
  public void shutdown() {
    // nichts freizugeben
  }
}
