package io.github.localsoul.port.engine.api.component;

import io.github.localsoul.port.engine.api.math.Quat;
import io.github.localsoul.port.engine.api.math.Vec3;

/**
 * Lokale Transformation relativ zum Elternobjekt (ohne Elternobjekt: relativ zur Welt). Die
 * Welttransformation ist {@code Eltern * T * R * S}. Rechts-händig, Y oben, Meter, Radiant
 * (ADR-007).
 *
 * @param position Position in Meter
 * @param rotation Rotation, normiert
 * @param scale Skalierung je Achse
 */
public record Transform(Vec3 position, Quat rotation, Vec3 scale) {

  /** Schemaversion. */
  public static final int SCHEMA_VERSION = 1;

  /** Default: Ursprung, keine Rotation, Skalierung 1. */
  public static final Transform IDENTITY = new Transform(Vec3.ZERO, Quat.IDENTITY, Vec3.ONE);

  /**
   * Transform an einer Position, sonst Default.
   *
   * @param position Position in Meter
   * @return Transform
   */
  public static Transform at(Vec3 position) {
    return new Transform(position, Quat.IDENTITY, Vec3.ONE);
  }

  /**
   * Kopie mit anderer Position.
   *
   * @param newPosition Position in Meter
   * @return neues Transform
   */
  public Transform withPosition(Vec3 newPosition) {
    return new Transform(newPosition, rotation, scale);
  }

  /**
   * Kopie mit anderer Rotation.
   *
   * @param newRotation Rotation
   * @return neues Transform
   */
  public Transform withRotation(Quat newRotation) {
    return new Transform(position, newRotation, scale);
  }

  /**
   * Kopie mit anderer Skalierung.
   *
   * @param newScale Skalierung
   * @return neues Transform
   */
  public Transform withScale(Vec3 newScale) {
    return new Transform(position, rotation, newScale);
  }
}
