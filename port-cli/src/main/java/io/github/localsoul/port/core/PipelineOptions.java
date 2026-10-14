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
 */
public record PipelineOptions(
    Phase from, boolean only, Path adapter, String classFilter, boolean rebuild) {}
