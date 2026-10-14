package io.github.localsoul.port.index;

import java.util.List;

/**
 * Ergebnis einer Aktualisierung.
 *
 * @param added neu eingelesene Dateien
 * @param changed geänderte Dateien
 * @param removed entfernte Dateien
 * @param symbols Anzahl der Symbole danach
 * @param changes Änderungen je Symbol dieses Laufs, sortiert
 */
public record UpdateResult(
    List<String> added,
    List<String> changed,
    List<String> removed,
    int symbols,
    List<Change> changes) {

  /** Kopiert die Listen. */
  public UpdateResult {
    added = List.copyOf(added);
    changed = List.copyOf(changed);
    removed = List.copyOf(removed);
    changes = List.copyOf(changes);
  }

  /**
   * {@code true}, wenn keine Datei neu gelesen oder entfernt wurde.
   *
   * @return nichts geändert
   */
  public boolean unchanged() {
    return added.isEmpty() && changed.isEmpty() && removed.isEmpty();
  }
}
