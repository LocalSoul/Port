package io.github.localsoul.port.engine.core.subsystem;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.subsystem.ComponentType;
import io.github.localsoul.port.engine.api.subsystem.EventBus;
import io.github.localsoul.port.engine.api.subsystem.EventType;
import io.github.localsoul.port.engine.api.subsystem.Subsystem;
import io.github.localsoul.port.engine.api.subsystem.SubsystemInfo;
import io.github.localsoul.port.engine.api.world.World;
import java.util.Set;
import java.util.function.Consumer;

/** Konfigurierbares Subsystem für Tests. */
public final class TestSubsystem implements Subsystem {

  private final SubsystemInfo info;
  private final Consumer<FrameContext> behavior;
  private EventBus bus;

  private TestSubsystem(SubsystemInfo info, Consumer<FrameContext> behavior) {
    this.info = info;
    this.behavior = behavior;
  }

  /** Subsystem ohne Verhalten, nur mit Deklaration. */
  public static TestSubsystem declaring(
      String id,
      String kind,
      Set<ComponentType> writes,
      Set<String> requires,
      Set<String> conflicts) {
    SubsystemInfo info =
        new SubsystemInfo(id, kind, Set.of(), writes, Set.of(), Set.of(), requires, conflicts);
    return new TestSubsystem(info, ctx -> {});
  }

  /** Subsystem mit Ereignis-Deklaration und Verhalten. */
  public static TestSubsystem withEvents(
      String id,
      Set<EventType> publishes,
      Set<EventType> consumes,
      Consumer<FrameContext> behavior) {
    SubsystemInfo info =
        new SubsystemInfo(id, id, Set.of(), Set.of(), publishes, consumes, Set.of(), Set.of());
    return new TestSubsystem(info, behavior);
  }

  /** Bus aus {@code init}, prüft Deklarationen. */
  public EventBus bus() {
    return bus;
  }

  @Override
  public SubsystemInfo info() {
    return info;
  }

  @Override
  public void init(World world, EventBus bus) {
    this.bus = bus;
  }

  @Override
  public void run(FrameContext context) {
    behavior.accept(context);
  }

  @Override
  public void shutdown() {}
}
