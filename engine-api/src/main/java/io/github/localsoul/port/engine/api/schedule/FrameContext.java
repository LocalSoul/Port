package io.github.localsoul.port.engine.api.schedule;

/** Kontext eines Phasenschritts, wird Callbacks und Subsystemen übergeben. */
public interface FrameContext {

  /**
   * Liefert die laufende Phase.
   *
   * @return die laufende Phase
   */
  Phase phase();

  /**
   * Liefert die Zeitwerte des laufenden Frames.
   *
   * @return die Zeitwerte des laufenden Frames
   */
  Time time();
}
