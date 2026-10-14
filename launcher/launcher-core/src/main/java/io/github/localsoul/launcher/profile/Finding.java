package io.github.localsoul.launcher.profile;

/**
 * Ergebnis einer Einzelprüfung von {@link Compatibility}.
 *
 * @param check Name der Prüfung, z. B. {@code adapter}
 * @param ok bestanden
 * @param message Meldung
 * @param location Ort oder {@code null}
 * @param hint nächster Befehl bei Fehlschlag (ADR-018) oder {@code null}
 */
public record Finding(String check, boolean ok, String message, String location, String hint) {}
