package io.github.localsoul.port.engine.api;

import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.schedule.PhaseCallback;
import io.github.localsoul.port.engine.api.world.World;

/**
 * Fassade der Engine für Adapter: Welt, Callbacks je Phase, Frame-Schritte. Erzeugt wird sie von
 * der Core-Implementierung; die API kennt keine Implementierung.
 */
public interface Engine {

  /**
   * Liefert die Welt.
   *
   * @return die Welt
   */
  World world();

  /**
   * Registriert einen Callback für eine Phase; er läuft nach allen vorher registrierten Callbacks
   * derselben Phase.
   *
   * @param phase Phase
   * @param callback Callback
   */
  void register(Phase phase, PhaseCallback callback);

  /**
   * Führt Frames mit der konfigurierten Frame-Dauer aus, ohne Wanduhr und ohne Fenster.
   *
   * @param frames Anzahl der Frames, nicht negativ
   * @throws IllegalArgumentException bei negativer Anzahl
   * @throws IllegalStateException nach {@link #shutdown()} oder bei Aufruf aus einem Callback
   */
  void step(int frames);

  /**
   * Liefert die Anzahl der bisher ausgeführten Frames.
   *
   * @return Frame-Zähler
   */
  long frameCount();

  /** Beendet die Engine; weitere {@link #step(int)}-Aufrufe sind nicht erlaubt. */
  void shutdown();
}
