package io.github.localsoul.port.engine.api.schedule;

/**
 * Phasen eines Frames, in dieser Reihenfolge: {@code EARLY}, dann 0 bis n mal {@code FIXED}, dann
 * {@code UPDATE}, dann {@code LATE}.
 */
public enum Phase {
  /** Frame-Beginn: Eingaben übernehmen, Vorbereitung. Einmal pro Frame. */
  EARLY,
  /** Fester Zeitschritt (z. B. Physik). 0 bis n mal pro Frame, je nach Akkumulator. */
  FIXED,
  /** Spiellogik mit variablem Zeitschritt. Einmal pro Frame. */
  UPDATE,
  /** Frame-Ende: abgeleitete Daten (Welttransformationen), Rendering. Einmal pro Frame. */
  LATE
}
