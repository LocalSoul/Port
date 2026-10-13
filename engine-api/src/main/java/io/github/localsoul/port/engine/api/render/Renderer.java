package io.github.localsoul.port.engine.api.render;

import io.github.localsoul.port.engine.api.subsystem.Subsystem;

/** Subsystem der Art {@code render}. Zeichnet in {@code LATE}, nach dem Transform-System. */
public interface Renderer extends Subsystem {

  /**
   * Liefert die Anzahl der bisher gezeichneten Frames.
   *
   * @return Anzahl
   */
  long framesRendered();
}
