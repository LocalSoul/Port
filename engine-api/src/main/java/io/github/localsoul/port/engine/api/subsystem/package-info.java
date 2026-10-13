/**
 * Subsystem-Vertrag (ADR-015): Subsysteme deklarieren in {@link
 * io.github.localsoul.port.engine.api.subsystem.SubsystemInfo}, welche Komponenten sie lesen und
 * schreiben und welche Ereignisse sie senden und empfangen. Sie sprechen nie direkt miteinander,
 * nur über Welt-Komponenten und den {@link io.github.localsoul.port.engine.api.subsystem.EventBus}.
 */
package io.github.localsoul.port.engine.api.subsystem;
