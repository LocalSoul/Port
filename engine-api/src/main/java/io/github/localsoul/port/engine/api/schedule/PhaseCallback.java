package io.github.localsoul.port.engine.api.schedule;

/** Wird in einer Phase aufgerufen. Callbacks einer Phase laufen in Registrierungsreihenfolge. */
@FunctionalInterface
public interface PhaseCallback {

  /**
   * Führt die Arbeit dieser Phase aus.
   *
   * @param context Zugriff auf Phase, Zeit und Engine-Zustand
   */
  void run(FrameContext context);
}
