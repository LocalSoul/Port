package io.github.localsoul.port.frontend.unity;

import io.github.localsoul.port.ir.ast.IrFile;
import java.util.Collections;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Ergebnis einer Übersetzung.
 *
 * @param files gelesene Dateien je Typname (sortiert, unveränderlich)
 * @param canonicalTexts kanonischer IR-Text je Typname (sortiert, unveränderlich)
 * @param summary Ausgabe des Programms auf stderr (raw-Stellen, Zusammenfassung)
 */
public record TranslationResult(
    SortedMap<String, IrFile> files, SortedMap<String, String> canonicalTexts, String summary) {

  /** Kopiert die Maps sortiert und unveränderlich. */
  public TranslationResult {
    files = Collections.unmodifiableSortedMap(new TreeMap<>(files));
    canonicalTexts = Collections.unmodifiableSortedMap(new TreeMap<>(canonicalTexts));
  }
}
