package io.github.localsoul.port.engine.api.subsystem;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.world.World;

/**
 * Ein austauschbares Subsystem (Input, Rendering, Physik, Audio, ...). Subsysteme laufen in jedem
 * Phasenschritt vor den Callbacks, in Registrierungsreihenfolge, und entscheiden selbst anhand von
 * {@link FrameContext#phase()}, ob sie etwas tun. Sie importieren und rufen sich nie gegenseitig
 * auf (ADR-015).
 */
public interface Subsystem {

  /**
   * Liefert die Deklaration; muss bei jedem Aufruf gleich sein.
   *
   * @return Deklaration
   */
  SubsystemInfo info();

  /**
   * Wird einmal beim Start aufgerufen, außerhalb eines Frames (Strukturänderungen wirken sofort).
   *
   * @param world Welt
   * @param bus Event-Bus dieses Subsystems; prüft die deklarierten Ereignistypen
   */
  void init(World world, EventBus bus);

  /**
   * Wird in jedem Phasenschritt aufgerufen.
   *
   * @param context Kontext des Phasenschritts
   */
  void run(FrameContext context);

  /** Wird beim Beenden der Engine aufgerufen. */
  void shutdown();
}
