package io.github.localsoul.port.engine.core.schedule;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.schedule.Time;

/** Erzeugt den Kontext eines Phasenschritts. */
@FunctionalInterface
public interface FrameContextFactory {

  /**
   * Erzeugt den Kontext.
   *
   * @param phase laufende Phase
   * @param time Zeitwerte des Frames
   * @return Kontext für Hooks und Callbacks
   */
  FrameContext create(Phase phase, Time time);
}
