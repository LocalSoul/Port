package io.github.localsoul.port.engine.core;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.core.schedule.PhaseHooks;
import io.github.localsoul.port.engine.core.transform.TransformSystem;
import io.github.localsoul.port.engine.core.world.CoreWorld;

/**
 * Verbindet Scheduler und Welt: Vor jedem Phasenschritt beginnt das Sammeln von Strukturänderungen,
 * danach werden sie ausgeführt. Zu Beginn von {@code LATE} läuft das Transform-System.
 */
final class FrameHooks implements PhaseHooks {

  private final CoreWorld world;

  FrameHooks(CoreWorld world) {
    this.world = world;
  }

  @Override
  public void beforeCallbacks(FrameContext context) {
    if (context.phase() == Phase.LATE) {
      TransformSystem.update(world);
    }
    world.beginPhase();
  }

  @Override
  public void afterStep(Phase phase) {
    world.endPhase();
  }
}
