package io.github.localsoul.port.engine.core.schedule;

import io.github.localsoul.port.engine.api.input.Input;
import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.schedule.PhaseCallback;
import io.github.localsoul.port.engine.api.schedule.Time;
import io.github.localsoul.port.engine.api.subsystem.EventBus;
import io.github.localsoul.port.engine.api.world.World;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Führt Frames aus: {@code EARLY → FIXED×n → UPDATE → LATE}.
 *
 * <ul>
 *   <li>Callbacks einer Phase laufen in Registrierungsreihenfolge.
 *   <li>Fixed-Timestep: Pro Frame wird die Frame-Dauer zum Akkumulator addiert; solange er
 *       mindestens {@code fixedDelta} enthält, läuft ein {@code FIXED}-Schritt und {@code
 *       fixedDelta} wird abgezogen. Der Rest bleibt für den nächsten Frame. Der Akkumulator rechnet
 *       intern in {@code double}, damit sich Rundungsfehler nicht über viele Frames aufsummieren.
 *   <li>Toleranz: Ein {@code FIXED}-Schritt läuft schon, wenn der Akkumulator mindestens {@code
 *       fixedDelta * (1 - }{@value #STEP_TOLERANCE}{@code )} enthält. Grund: Frame-Dauern kommen
 *       als {@code float} und liegen oft knapp unter dem gemeinten Vielfachen ({@code 2.5f * 0.02f}
 *       ist {@code 0.0499999970}). Ohne Toleranz rutscht dann gelegentlich ein Schritt in den
 *       nächsten Frame. Der Akkumulator kann dadurch minimal negativ werden.
 *   <li>Um jeden Phasenschritt laufen die {@link PhaseHooks} des Cores.
 * </ul>
 *
 * <p>Ohne Wanduhr: Die Frame-Dauer wird bei {@link #runFrame(float)} übergeben.
 */
public final class Scheduler {

  /** Relative Toleranz beim Vergleich des Akkumulators mit {@code fixedDelta}. */
  public static final double STEP_TOLERANCE = 1e-4;

  private final float fixedDelta;
  private final double stepThreshold;
  private final Map<Phase, List<PhaseCallback>> callbacks = new EnumMap<>(Phase.class);
  private PhaseHooks hooks = PhaseHooks.NONE;
  private FrameContextFactory contextFactory = StepContext::new;
  private double accumulator;
  private long frame;
  private Phase currentPhase;

  /**
   * Erzeugt einen Scheduler.
   *
   * @param fixedDelta Länge eines festen Zeitschritts in Sekunden, größer als 0
   * @throws IllegalArgumentException wenn {@code fixedDelta} nicht positiv ist
   */
  public Scheduler(float fixedDelta) {
    if (!(fixedDelta > 0f)) {
      throw new IllegalArgumentException("fixedDelta muss größer als 0 sein: " + fixedDelta);
    }
    this.fixedDelta = fixedDelta;
    this.stepThreshold = fixedDelta * (1.0 - STEP_TOLERANCE);
    for (Phase phase : Phase.values()) {
      callbacks.put(phase, new ArrayList<>());
    }
  }

  /**
   * Setzt die Einhängepunkte des Cores.
   *
   * @param hooks Einhängepunkte, nicht {@code null}
   */
  public void setHooks(PhaseHooks hooks) {
    this.hooks = hooks;
  }

  /**
   * Setzt die Fabrik für den Kontext je Phasenschritt.
   *
   * @param contextFactory Fabrik, nicht {@code null}
   */
  public void setContextFactory(FrameContextFactory contextFactory) {
    this.contextFactory = contextFactory;
  }

  /**
   * Registriert einen Callback; er läuft nach allen vorher registrierten Callbacks derselben Phase.
   *
   * @param phase Phase
   * @param callback Callback
   */
  public void register(Phase phase, PhaseCallback callback) {
    callbacks.get(phase).add(callback);
  }

  /**
   * Führt einen Frame aus.
   *
   * @param frameDelta Dauer des Frames in Sekunden, nicht negativ
   * @throws IllegalArgumentException bei negativer Dauer
   * @throws IllegalStateException wenn bereits ein Frame läuft (Aufruf aus einem Callback)
   */
  public void runFrame(float frameDelta) {
    if (frameDelta < 0f) {
      throw new IllegalArgumentException("frameDelta darf nicht negativ sein: " + frameDelta);
    }
    if (currentPhase != null) {
      throw new IllegalStateException("runFrame während Phase " + currentPhase);
    }
    Time time = new Time(frameDelta, fixedDelta, frame);
    runStep(Phase.EARLY, time);
    accumulator += frameDelta;
    while (accumulator >= stepThreshold) {
      runStep(Phase.FIXED, time);
      accumulator -= fixedDelta;
    }
    runStep(Phase.UPDATE, time);
    runStep(Phase.LATE, time);
    frame++;
  }

  private void runStep(Phase phase, Time time) {
    currentPhase = phase;
    try {
      FrameContext context = contextFactory.create(phase, time);
      hooks.beforeCallbacks(context);
      List<PhaseCallback> phaseCallbacks = callbacks.get(phase);
      for (int i = 0; i < phaseCallbacks.size(); i++) {
        phaseCallbacks.get(i).run(context);
      }
    } finally {
      currentPhase = null;
    }
    hooks.afterStep(phase);
  }

  /**
   * Liefert die laufende Phase.
   *
   * @return laufende Phase oder {@code null} außerhalb eines Phasenschritts
   */
  public Phase currentPhase() {
    return currentPhase;
  }

  /**
   * Liefert die Nummer des nächsten Frames (Anzahl bisher ausgeführter Frames).
   *
   * @return Frame-Zähler
   */
  public long frameCount() {
    return frame;
  }

  /**
   * Liefert den Rest im Akkumulator, der noch keinen {@code FIXED}-Schritt ergibt.
   *
   * @return Rest in Sekunden, kleiner als {@code fixedDelta} (wegen der Toleranz eventuell minimal
   *     negativ)
   */
  public double accumulator() {
    return accumulator;
  }

  /**
   * Liefert die Länge eines festen Zeitschritts.
   *
   * @return Länge in Sekunden
   */
  public float fixedDelta() {
    return fixedDelta;
  }

  /** Kontext ohne Engine-Zustand, für einen Scheduler ohne Welt (Tests). */
  private record StepContext(Phase phase, Time time) implements FrameContext {

    @Override
    public World world() {
      throw new IllegalStateException("Scheduler ohne Welt: keine Kontextfabrik gesetzt");
    }

    @Override
    public Input input() {
      throw new IllegalStateException("Scheduler ohne Input: keine Kontextfabrik gesetzt");
    }

    @Override
    public EventBus events() {
      throw new IllegalStateException("Scheduler ohne Event-Bus: keine Kontextfabrik gesetzt");
    }
  }
}
