package io.github.localsoul.launcher.catalog;

/**
 * Meldung des Katalogs zu einem defekten Unterprojekt; der Katalog bricht dabei nicht ab.
 *
 * @param id Diagnose-ID
 * @param message Meldung
 * @param location Ort relativ zur Wurzel
 * @param hint nächster Schritt oder {@code null}
 */
public record Problem(String id, String message, String location, String hint) {}
