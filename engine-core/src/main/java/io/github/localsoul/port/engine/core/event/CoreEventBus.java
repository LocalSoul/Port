package io.github.localsoul.port.engine.core.event;

import io.github.localsoul.port.engine.api.subsystem.Event;
import io.github.localsoul.port.engine.api.subsystem.EventBus;
import io.github.localsoul.port.engine.api.subsystem.EventType;
import java.util.ArrayList;
import java.util.List;

/**
 * Event-Bus mit zwei Puffern: Veröffentlichtes landet in {@code pending}; {@link #endStep()} macht
 * es zum sichtbaren Puffer des nächsten Phasenschritts und verwirft die bisher sichtbaren
 * Ereignisse. Sequenznummern laufen über die ganze Lebensdauer fort.
 */
public final class CoreEventBus implements EventBus {

  private List<Event> visible = new ArrayList<>();
  private List<Event> pending = new ArrayList<>();
  private long nextSequence;

  /** Erzeugt einen leeren Bus. */
  public CoreEventBus() {}

  @Override
  public void publish(EventType type, Object payload) {
    pending.add(new Event(type, payload, nextSequence));
    nextSequence++;
  }

  @Override
  public List<Event> events(EventType type) {
    List<Event> result = new ArrayList<>();
    for (Event event : visible) {
      if (event.type().equals(type)) {
        result.add(event);
      }
    }
    return List.copyOf(result);
  }

  /**
   * Liefert alle sichtbaren Ereignisse.
   *
   * @return Ereignisse in Veröffentlichungsreihenfolge
   */
  public List<Event> visibleEvents() {
    return List.copyOf(visible);
  }

  /** Beendet einen Phasenschritt: Veröffentlichtes wird im nächsten Schritt sichtbar. */
  public void endStep() {
    visible = pending;
    pending = new ArrayList<>();
  }
}
