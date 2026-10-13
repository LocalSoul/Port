package io.github.localsoul.port.output;

import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import io.github.localsoul.port.json.JsonWriter;
import java.util.ArrayList;
import java.util.List;

/** JSON-Hülle laut {@code docs/cli-contract.md}: genau ein Objekt. */
public final class JsonRenderer {

  /** Version der Hülle. */
  public static final String SCHEMA = "port/1";

  private JsonRenderer() {}

  /**
   * Erzeugt die Hülle {@code schema, command, ok, exitCode, data, diagnostics}.
   *
   * @param result Ergebnis
   * @param exitCode abgeleiteter Exit-Code
   * @return JSON-Text mit abschließendem Zeilenumbruch
   */
  public static String render(CommandResult result, ExitCode exitCode) {
    List<JsonValue> diagnostics = new ArrayList<>();
    for (Diagnostic diagnostic : result.diagnostics()) {
      diagnostics.add(diagnostic(diagnostic));
    }
    JsonObject envelope =
        JsonObject.builder()
            .put("schema", SCHEMA)
            .put("command", result.command())
            .put("ok", exitCode == ExitCode.SUCCESS)
            .put("exitCode", exitCode.code())
            .put("data", result.data())
            .put("diagnostics", new JsonValue.JsonArray(diagnostics))
            .build();
    return JsonWriter.write(envelope);
  }

  private static JsonObject diagnostic(Diagnostic diagnostic) {
    return JsonObject.builder()
        .put("id", diagnostic.id())
        .put("severity", diagnostic.severity().spelling())
        .put("symbol", diagnostic.symbol())
        .put("message", diagnostic.message())
        .put("location", diagnostic.location())
        .put("hint", diagnostic.hint())
        .build();
  }
}
