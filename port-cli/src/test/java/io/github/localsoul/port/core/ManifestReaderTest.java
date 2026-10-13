package io.github.localsoul.port.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

/** Ungültige {@code port.toml} → Exit 2 mit Ort und verständlicher Meldung. */
class ManifestReaderTest {

  private static final String VALID =
      """
      [project]
      kind = "game"
      name = "demo"

      [game]
      frontend = "unity"
      source = "input/Assembly-CSharp.dll"

      [game.requires]
      adapter = "unity"
      api = ">=2022.3"
      subsystems = ["scene", "input"]

      [lint]
      disable = ["SM007"]
      """;

  private static Diagnostic failure(String text) {
    PortException e = assertThrows(PortException.class, () -> ManifestReader.parse(text));
    assertEquals(ExitCode.INVALID, e.exitCode());
    assertEquals(DiagnosticIds.MANIFEST, e.diagnostic().id());
    return e.diagnostic();
  }

  @Test
  void readsValidManifestAndIgnoresLaterSections() throws Exception {
    Manifest manifest = ManifestReader.parse(VALID);
    assertEquals("demo", manifest.name());
    assertEquals(List.of("scene", "input"), manifest.subsystems());
    assertEquals(null, manifest.sha256());
  }

  @Test
  void writtenManifestReadsBack() throws Exception {
    Manifest manifest = Manifest.newGame("demo", "unity", "input/a.dll", "0".repeat(64));
    assertEquals(manifest, ManifestReader.parse(manifest.toToml()));
  }

  @Test
  void syntaxErrorHasLineAndColumn() {
    Diagnostic d = failure("[project]\nkind = \"game\nname = 1\n");
    assertEquals("port.toml:2:13", d.location());
  }

  @Test
  void missingFieldWrongTypeAndWrongValue() {
    assertEquals(
        "Pflichtfeld fehlt: game.source", failure(VALID.replace("source =", "#")).message());
    Diagnostic type = failure(VALID.replace("name = \"demo\"", "name = 5"));
    assertEquals("port.toml:3:1", type.location());
    failure(VALID.replace("kind = \"game\"", "kind = \"engine\""));
    failure(VALID.replace("frontend = \"unity\"", "frontend = \"godot\""));
    failure(VALID.replace("[game]\n", "[game]\nsha256 = \"abc\"\n"));
    failure(VALID.replace("[\"scene\", \"input\"]", "[1, 2]"));
  }
}
