/**
 * Scheduler-Vertrag: Phasen, Zeit und Callbacks. Ein Frame läuft immer {@code EARLY → FIXED×n →
 * UPDATE → LATE}; Lifecycle-Begriffe fremder Engines bilden Adapter darauf ab.
 */
package io.github.localsoul.port.engine.api.schedule;
