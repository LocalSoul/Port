package io.github.localsoul.port.output;

import io.github.localsoul.port.cli.Golden;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.core.Severity;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Hülle laut {@code docs/cli-contract.md} als Golden File. */
class JsonRendererTest {

  @Test
  void successEnvelopeWithSortedDiagnostics() throws Exception {
    JsonObject data =
        JsonObject.builder()
            .put("files", JsonValue.strings(List.of("gen/a/Foo.java", "gen/a/FooBase.java")))
            .put("todoCount", 1)
            .build();
    List<Diagnostic> diagnostics =
        List.of(
            new Diagnostic(
                "PORT-TODO", Severity.WARN, "Foo::Bar#", "externes Symbol", "ir/Foo.ir:10:3", null),
            new Diagnostic(
                "PORT-TODO", Severity.WARN, "Foo::Baz#", "Lambda", "ir/Foo.ir:9:12", null),
            Diagnostic.info("PORT-LATER", "geänderte IR-Knoten brauchen den Index (M5)"));
    String json = JsonRenderer.render(CommandResult.ok("gen", data, diagnostics), ExitCode.SUCCESS);
    Golden.assertMatches("envelope-success.json", json);
  }

  @Test
  void failureEnvelope() throws Exception {
    Diagnostic hash =
        Diagnostic.error(
            "PORT-HASH",
            "SHA-256 von input/Assembly-CSharp.dll stimmt nicht mit port.toml überein",
            "input/Assembly-CSharp.dll",
            "Eingabe prüfen oder sha256 in port.toml anpassen");
    CommandResult result =
        CommandResult.failed("extract", JsonObject.empty(), List.of(hash), ExitCode.INPUT);
    Golden.assertMatches("envelope-failure.json", JsonRenderer.render(result, ExitCode.INPUT));
  }
}
