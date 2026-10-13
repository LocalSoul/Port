package io.github.localsoul.port.engine.core;

import io.github.localsoul.port.engine.api.Engine;
import io.github.localsoul.port.engine.api.input.Input;
import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.schedule.PhaseCallback;
import io.github.localsoul.port.engine.api.schedule.Time;
import io.github.localsoul.port.engine.api.subsystem.EventBus;
import io.github.localsoul.port.engine.api.subsystem.Subsystem;
import io.github.localsoul.port.engine.api.world.World;
import io.github.localsoul.port.engine.core.event.CoreEventBus;
import io.github.localsoul.port.engine.core.event.DeclaredEventBus;
import io.github.localsoul.port.engine.core.input.InputView;
import io.github.localsoul.port.engine.core.schedule.Scheduler;
import io.github.localsoul.port.engine.core.subsystem.SubsystemRegistry;
import io.github.localsoul.port.engine.core.world.CoreWorld;
import java.util.ArrayList;
import java.util.List;

/**
 * Headless-Engine: Welt, Scheduler, Event-Bus, Subsysteme und Core-Systeme. Erzeugung über {@link
 * #builder()}.
 *
 * <p>Beim {@link Builder#build()} wird die Subsystem-Kombination geprüft ({@link
 * SubsystemRegistry}), danach wird jedes Subsystem in Registrierungsreihenfolge initialisiert.
 * Ablauf je Phasenschritt siehe {@link FrameHooks}.
 */
public final class CoreEngine implements Engine {

  private final CoreWorld world;
  private final Scheduler scheduler;
  private final CoreEventBus bus;
  private final Input input;
  private final List<Subsystem> subsystems;
  private final float frameDelta;
  private boolean shutDown;

  private CoreEngine(Builder builder) {
    this.world = new CoreWorld();
    this.scheduler = new Scheduler(builder.fixedDelta);
    this.bus = new CoreEventBus();
    this.input = new InputView(world);
    this.subsystems = List.copyOf(builder.subsystems);
    this.frameDelta = builder.frameDelta;
    scheduler.setHooks(new FrameHooks(world, bus, subsystems));
    scheduler.setContextFactory(this::createContext);
  }

  /**
   * Liefert einen Builder mit Defaults: Fixed-Delta 1/50 s, Frame-Dauer 1/60 s, keine Subsysteme.
   *
   * @return neuer Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  private void initSubsystems() {
    for (Subsystem subsystem : subsystems) {
      subsystem.init(world, new DeclaredEventBus(bus, subsystem.info()));
    }
  }

  private FrameContext createContext(Phase phase, Time time) {
    return new CoreFrameContext(phase, time, world, input, bus);
  }

  @Override
  public World world() {
    return world;
  }

  /**
   * Liefert den gemeinsamen Event-Bus (ohne Deklarationsprüfung, für Adapter).
   *
   * @return Event-Bus
   */
  public EventBus events() {
    return bus;
  }

  /**
   * Liefert die Eingabe-Sicht.
   *
   * @return Eingabe
   */
  public Input input() {
    return input;
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

  /** Beendet die Engine und ruft {@code shutdown} der Subsysteme in umgekehrter Reihenfolge. */
  @Override
  public void shutdown() {
    if (shutDown) {
      return;
    }
    shutDown = true;
    for (int i = subsystems.size() - 1; i >= 0; i--) {
      subsystems.get(i).shutdown();
    }
  }

  /** Kontext eines Phasenschritts. */
  private record CoreFrameContext(Phase phase, Time time, World world, Input input, EventBus events)
      implements FrameContext {}

  /** Konfiguration der Engine. */
  public static final class Builder {

    private float fixedDelta = 1f / 50f;
    private float frameDelta = 1f / 60f;
    private final List<Subsystem> subsystems = new ArrayList<>();

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
     * Fügt ein Subsystem hinzu; Subsysteme laufen in dieser Reihenfolge.
     *
     * @param subsystem Subsystem
     * @return dieser Builder
     */
    public Builder subsystem(Subsystem subsystem) {
      subsystems.add(subsystem);
      return this;
    }

    /**
     * Prüft die Konfiguration, erzeugt die Engine und initialisiert die Subsysteme.
     *
     * @return neue Engine
     * @throws IllegalArgumentException bei ungültigen Zeitwerten
     * @throws io.github.localsoul.port.engine.api.subsystem.SubsystemConfigurationException bei
     *     ungültiger Subsystem-Kombination
     */
    public CoreEngine build() {
      if (frameDelta < 0f) {
        throw new IllegalArgumentException("frameDelta darf nicht negativ sein: " + frameDelta);
      }
      SubsystemRegistry.validate(subsystems);
      CoreEngine engine = new CoreEngine(this);
      engine.initSubsystems();
      return engine;
    }
  }
}
