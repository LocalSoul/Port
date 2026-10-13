package io.github.localsoul.port.engine.api.schedule;

/**
 * Zeitwerte des laufenden Frames, in Sekunden.
 *
 * @param delta Dauer des aktuellen Frames (in allen Phasen gleich, auch in {@code FIXED})
 * @param fixedDelta Länge eines festen Zeitschritts
 * @param frame Nummer des aktuellen Frames, der erste Frame hat die Nummer 0
 */
public record Time(float delta, float fixedDelta, long frame) {}
