package io.github.localsoul.port.core;

import io.github.localsoul.port.index.IndexException;
import io.github.localsoul.port.index.IrIndex;
import io.github.localsoul.port.index.UpdateResult;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.util.List;

/**
 * {@code index}: aktualisiert {@code .port/index.db} inkrementell aus {@code ir/} ({@code
 * --rebuild}: neu aufbauen). Daten: gelesene, geänderte, entfernte Dateien, Symbolzahl, Anzahl der
 * Änderungen je Symbol.
 */
final class IndexStep implements PhaseStep {

  @Override
  public JsonObject run(
      Project project,
      PipelineOptions options,
      CommandContext context,
      List<Diagnostic> diagnostics)
      throws PortException {
    UpdateResult result;
    try (IrIndex index = IrIndex.open(project.dir(), options.rebuild())) {
      result = index.update();
    } catch (IndexException e) {
      throw IndexErrors.toPort(e);
    }
    return JsonObject.builder()
        .put("added", JsonValue.strings(result.added()))
        .put("changed", JsonValue.strings(result.changed()))
        .put("removed", JsonValue.strings(result.removed()))
        .put("symbols", result.symbols())
        .put("symbolChanges", result.changes().size())
        .build();
  }
}
