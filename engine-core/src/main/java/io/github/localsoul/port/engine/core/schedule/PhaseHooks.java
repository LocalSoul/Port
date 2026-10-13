package io.github.localsoul.port.engine.core.schedule;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;

/**
 * Einhängepunkte des Cores um jeden Phasenschritt (Subsysteme, Command-Buffer, Event-Puffer). Ein
 * Phasenschritt ist ein einzelner Durchlauf einer Phase, also auch jeder einzelne {@code FIXED}.
 */
public interface PhaseHooks {

  /** Keine Einhängepunkte. */
  PhaseHooks NONE = new PhaseHooks() {};

  /**
   * Läuft vor den Callbacks eines Phasenschritts.
   *
   * @param context Kontext des Schritts
   */
  default void beforeCallbacks(FrameContext context) {}

  /**
   * Läuft nach den Callbacks eines Phasenschritts.
   *
   * @param phase beendete Phase
   */
  default void afterStep(Phase phase) {}
}
