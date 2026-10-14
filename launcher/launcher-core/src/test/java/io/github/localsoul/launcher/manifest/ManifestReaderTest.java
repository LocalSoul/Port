package io.github.localsoul.launcher.manifest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.localsoul.launcher.LauncherException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Beide Manifestarten: gültig und jeder Fehlerfall → Exit 2 mit Ort. */
class ManifestReaderTest {

  private static final String GAME =
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

  private static final String ENGINE =
      """
      [project]
      kind = "engine"
      name = "unity"

      [engine.provides]
      adapter = "unity"
      api = "2022.3"
      core-api = ">=0.1 <0.2"
      providers = []

      [engine.build]
      module = "adapter-unity"

      [conformance]
      scenarios = "conformance/scenarios"
      """;

  private static LauncherException failure(String text) {
    LauncherException e =
        assertThrows(LauncherException.class, () -> ManifestReader.parse(text, "port.toml"));
    assertEquals(LauncherException.INVALID, e.exitCode());
    assertEquals("PORT-MANIFEST", e.id());
    return e;
  }

  @Test
  void readsGameAndIgnoresLaterSections() throws Exception {
    GameManifest game =
        assertInstanceOf(GameManifest.class, ManifestReader.parse(GAME, "port.toml"));
    assertEquals("demo", game.name());
    assertEquals(List.of("scene", "input"), game.subsystems());
    assertEquals(null, game.sha256());
  }

  @Test
  void readsEngine() throws Exception {
    EngineManifest engine =
        assertInstanceOf(EngineManifest.class, ManifestReader.parse(ENGINE, "port.toml"));
    assertEquals("unity", engine.adapter());
    assertEquals(">=0.1 <0.2", engine.coreApi());
    assertEquals("adapter-unity", engine.module());
  }

  @Test
  void writtenManifestsReadBack() throws Exception {
    GameManifest game = GameManifest.newGame("demo", "unity", "input/a.dll", "0".repeat(64));
    assertEquals(game, ManifestReader.parse(game.toToml(), "port.toml"));
    EngineManifest engine =
        new EngineManifest("godot", "godot", "4.2", ">=0.1", List.of("render-x"), "adapter-godot");
    assertEquals(engine, ManifestReader.parse(engine.toToml(), "port.toml"));
  }

  @Test
  void syntaxErrorHasLineAndColumn() {
    assertEquals("port.toml:2:13", failure("[project]\nkind = \"game\nname = 1\n").location());
  }

  @Test
  void gameErrors() {
    assertEquals(
        "Pflichtfeld fehlt: game.source", failure(GAME.replace("source =", "#")).getMessage());
    assertEquals("port.toml:3:1", failure(GAME.replace("name = \"demo\"", "name = 5")).location());
    failure(GAME.replace("name = \"demo\"", "name = \"Demo!\""));
    failure(GAME.replace("frontend = \"unity\"", "frontend = \"godot\""));
    failure(GAME.replace("[game]\n", "[game]\nsha256 = \"abc\"\n"));
    failure(GAME.replace("[\"scene\", \"input\"]", "[1, 2]"));
    failure(GAME.replace(">=2022.3", ">=neu"));
  }

  @Test
  void lintSectionIsReadAndChecked() throws Exception {
    String text = GAME + "\n[lint.severity]\nSM001 = \"error\"\nPT004 = \"info\"\n";
    GameManifest game =
        assertInstanceOf(GameManifest.class, ManifestReader.parse(text, "port.toml"));
    assertEquals(List.of("SM007"), game.lint().disable());
    assertEquals(java.util.Map.of("PT004", "info", "SM001", "error"), game.lint().severity());
    assertEquals(game, ManifestReader.parse(game.toToml(), "port.toml"));
    failure(GAME.replace("disable = [\"SM007\"]", "disable = [\"sm7\"]"));
    failure(GAME + "\n[lint.severity]\nSM001 = \"laut\"\n");
    failure(GAME + "\n[lint.severity]\nXYZ = \"warn\"\n");
    failure("lint = 3\n" + GAME.replace("[lint]\ndisable = [\"SM007\"]", ""));
    assertEquals(LintSettings.NONE, GameManifest.newGame("a", "unity", "input/a.dll", null).lint());
  }

  @Test
  void engineErrors() {
    failure(ENGINE.replace("kind = \"engine\"", "kind = \"plugin\""));
    failure(ENGINE.replace("api = \"2022.3\"", "api = \"neu\""));
    failure(ENGINE.replace(">=0.1 <0.2", "ab 0.1"));
    assertEquals(
        "Pflichtfeld fehlt: engine.build.module",
        failure(ENGINE.replace("module =", "#")).getMessage());
    failure(ENGINE.replace("providers = []", "providers = \"x\""));
  }
}
