package io.github.localsoul.port.frontend.unity;

import java.nio.file.Path;
import java.util.List;

/**
 * Auftrag an das Frontend.
 *
 * @param assembly zu übersetzende Assembly
 * @param classes Muster für Typnamen ({@code *}, {@code ?}); leer = alle Typen
 * @param references zusätzliche Verzeichnisse zur Typauflösung (Default des Programms: Verzeichnis
 *     der Assembly)
 * @param includeSha256 {@code :sha256} der Eingabe in den Kopf schreiben
 */
public record TranslationRequest(
    Path assembly, List<String> classes, List<Path> references, boolean includeSha256) {

  /** Kopiert die Listen. */
  public TranslationRequest {
    classes = List.copyOf(classes);
    references = List.copyOf(references);
  }

  /**
   * Auftrag für alle Typen mit Hash.
   *
   * @param assembly Assembly
   * @return Auftrag
   */
  public static TranslationRequest of(Path assembly) {
    return new TranslationRequest(assembly, List.of(), List.of(), true);
  }
}
