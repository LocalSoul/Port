package io.github.localsoul.port.engine.core;

import io.github.localsoul.port.engine.api.Engine;
import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.schedule.PhaseCallback;
import io.github.localsoul.port.engine.api.schedule.Time;
import io.github.localsoul.port.engine.api.world.World;
import io.github.localsoul.port.engine.core.schedule.Scheduler;
import io.github.localsoul.port.engine.core.world.CoreWorld;

/**
 * Headless-Engine: Welt, Scheduler und Core-Systeme. Erzeugung über {@link #builder()}.
 *
 * <p>Ablauf je Frame siehe {@link Scheduler}; um jeden Phasenschritt laufen die Core-Hooks
 * (Command-Buffer, Transform-System in {@code LATE}).
 */
public final class CoreEngine implements Engine {

  private final CoreWorld world;
  private final Scheduler scheduler;
  private final float frameDelta;
  private boolean shutDown;

  private CoreEngine(Builder builder) {
    this.world = new CoreWorld();
    this.scheduler = new Scheduler(builder.fixedDelta);
    this.frameDelta = builder.frameDelta;
    scheduler.setHooks(new FrameHooks(world));
    scheduler.setContextFactory(this::createContext);
  }

  /**
   * Liefert einen Builder mit Defaults: Fixed-Delta 1/50 s, Frame-Dauer 1/60 s.
   *
   * @return neuer Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  private FrameContext createContext(Phase phase, Time time) {
    return new CoreFrameContext(phase, time, world);
  }

  @Override
  public World world() {
    return world;
  }

  @Override
  public void register(Phase phase, PhaseCallback callback) {
    scheduler.register(phase, callback);
  }

  @Override
  public void step(int frames) {
    if (frames < 0) {
      throw new IllegalArgumentException("frames darf nicht negativ sein: " + frames);
    }
    if (shutDown) {
      throw new IllegalStateException("Engine ist beendet");
    }
    for (int i = 0; i < frames; i++) {
      scheduler.runFrame(frameDelta);
    }
  }

  @Override
  public long frameCount() {
    return scheduler.frameCount();
  }

  @Override
  public void shutdown() {
    shutDown = true;
  }

  /** Kontext eines Phasenschritts. */
  private record CoreFrameContext(Phase phase, Time time, World world) implements FrameContext {}

  /** Konfiguration der Engine. */
  public static final class Builder {

    private float fixedDelta = 1f / 50f;
    private float frameDelta = 1f / 60f;

    private Builder() {}

    /**
     * Setzt die Länge eines festen Zeitschritts.
     *
     * @param seconds Sekunden, größer als 0
     * @return dieser Builder
     */
    public Builder fixedDelta(float seconds) {
      this.fixedDelta = seconds;
      return this;
    }

    /**
     * Setzt die Dauer jedes Frames bei {@link CoreEngine#step(int)}.
     *
     * @param seconds Sekunden, nicht negativ
     * @return dieser Builder
     */
    public Builder frameDelta(float seconds) {
      this.frameDelta = seconds;
      return this;
    }

    /**
     * Erzeugt die Engine.
     *
     * @return neue Engine
     * @throws IllegalArgumentException bei ungültigen Zeitwerten
     */
    public CoreEngine build() {
      if (frameDelta < 0f) {
        throw new IllegalArgumentException("frameDelta darf nicht negativ sein: " + frameDelta);
      }
      return new CoreEngine(this);
    }
  }
}
