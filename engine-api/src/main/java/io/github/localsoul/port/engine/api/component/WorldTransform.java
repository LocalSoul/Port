package io.github.localsoul.port.engine.api.component;

import io.github.localsoul.port.engine.api.math.Mat4;

/**
 * Welttransformation, vom Core zu Beginn von {@code LATE} für jede Entity mit {@link Transform}
 * berechnet (Besitzer: Core). Für aktuelle Werte mitten im Frame {@code World.worldMatrix} nutzen.
 *
 * @param matrix Matrix von lokalen in Weltkoordinaten
 */
public record WorldTransform(Mat4 matrix) {

  /** Schemaversion. */
  public static final int SCHEMA_VERSION = 1;
}
