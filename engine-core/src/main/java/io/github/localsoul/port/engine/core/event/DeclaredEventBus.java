package io.github.localsoul.port.engine.core.event;

import io.github.localsoul.port.engine.api.subsystem.Event;
import io.github.localsoul.port.engine.api.subsystem.EventBus;
import io.github.localsoul.port.engine.api.subsystem.EventType;
import io.github.localsoul.port.engine.api.subsystem.SubsystemConfigurationException;
import io.github.localsoul.port.engine.api.subsystem.SubsystemInfo;
import java.util.List;

/** Sicht eines Subsystems auf den Bus: erlaubt nur deklarierte Ereignistypen. */
public final class DeclaredEventBus implements EventBus {

  private final EventBus target;
  private final SubsystemInfo info;

  /**
   * Erzeugt die Sicht.
   *
   * @param target gemeinsamer Bus
   * @param info Deklaration des Subsystems
   */
  public DeclaredEventBus(EventBus target, SubsystemInfo info) {
    this.target = target;
    this.info = info;
  }

  @Override
  public void publish(EventType type, Object payload) {
    if (!info.publishes().contains(type)) {
      throw new SubsystemConfigurationException(
          "Subsystem '"
              + info.id()
              + "' veröffentlicht "
              + type
              + ", hat es aber nicht deklariert");
    }
    target.publish(type, payload);
  }

  @Override
  public List<Event> events(EventType type) {
    if (!info.consumes().contains(type)) {
      throw new SubsystemConfigurationException(
          "Subsystem '" + info.id() + "' liest " + type + ", hat es aber nicht deklariert");
    }
    return target.events(type);
  }
}
