package io.github.localsoul.port.engine.core;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.subsystem.Subsystem;
import io.github.localsoul.port.engine.core.event.CoreEventBus;
import io.github.localsoul.port.engine.core.schedule.PhaseHooks;
import io.github.localsoul.port.engine.core.transform.TransformSystem;
import io.github.localsoul.port.engine.core.world.CoreWorld;
import java.util.List;

/**
 * Ablauf um jeden Phasenschritt:
 *
 * <ol>
 *   <li>nur in {@code LATE}: Transform-System schreibt {@code WorldTransform},
 *   <li>Sammeln von Strukturänderungen beginnt,
 *   <li>Subsysteme laufen in Registrierungsreihenfolge,
 *   <li>(Callbacks laufen, im Scheduler),
 *   <li>Strukturänderungen werden ausgeführt, Event-Puffer werden getauscht.
 * </ol>
 */
final class FrameHooks implements PhaseHooks {

  private final CoreWorld world;
  private final CoreEventBus bus;
  private final List<Subsystem> subsystems;

  FrameHooks(CoreWorld world, CoreEventBus bus, List<Subsystem> subsystems) {
    this.world = world;
    this.bus = bus;
    this.subsystems = List.copyOf(subsystems);
  }

  @Override
  public void beforeCallbacks(FrameContext context) {
    if (context.phase() == Phase.LATE) {
      TransformSystem.update(world);
    }
    world.beginPhase();
    for (int i = 0; i < subsystems.size(); i++) {
      subsystems.get(i).run(context);
    }
  }

  @Override
  public void afterStep(Phase phase) {
    try {
      world.endPhase();
    } finally {
      bus.endStep();
    }
  }
}
