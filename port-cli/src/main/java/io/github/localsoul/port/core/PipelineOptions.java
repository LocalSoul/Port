package io.github.localsoul.port.core;

import java.nio.file.Path;

/**
 * Optionen der Lifecycle-Befehle.
 *
 * @param from erste Phase
 * @param only nur {@code from} ausführen, keine Folgephasen
 * @param adapter Adapter-Ordner ({@code --adapter}) oder {@code null} für die Konvention
 * @param classFilter Typmuster für {@code gen --class} oder {@code null}
 * @param rebuild {@code index --rebuild}: Index verwerfen und neu aufbauen
 * @param lint Optionen von {@code port lint}
 */
public record PipelineOptions(
    Phase from, boolean only, Path adapter, String classFilter, boolean rebuild, LintOptions lint) {

  /**
   * Optionen von {@code port lint}.
   *
   * @param rules nur diese Regeln ({@code --rule}), leer = alle
   * @param classFilter nur Befunde zu Typen, die dem Muster entsprechen ({@code --class})
   * @param fixHints Hinweise zur Behebung ausgeben ({@code --fix-hint})
   */
  public record LintOptions(java.util.List<String> rules, String classFilter, boolean fixHints) {

    /** Standard: alle Regeln, alle Typen, ohne Hinweise. */
    public static final LintOptions DEFAULT = new LintOptions(java.util.List.of(), null, false);

    /** Kopiert die Liste. */
    public LintOptions {
      rules = java.util.List.copyOf(rules);
    }
  }
}
