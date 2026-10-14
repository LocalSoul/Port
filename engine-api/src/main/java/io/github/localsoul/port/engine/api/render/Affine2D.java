package io.github.localsoul.port.engine.api.render;

/**
 * Affine Abbildung in 2D: {@code x' = m00·x + m01·y + m02}, {@code y' = m10·x + m11·y + m12}.
 *
 * @param m00 Zeile 0, Spalte 0
 * @param m01 Zeile 0, Spalte 1
 * @param m02 Verschiebung x
 * @param m10 Zeile 1, Spalte 0
 * @param m11 Zeile 1, Spalte 1
 * @param m12 Verschiebung y
 */
public record Affine2D(double m00, double m01, double m02, double m10, double m11, double m12) {}
