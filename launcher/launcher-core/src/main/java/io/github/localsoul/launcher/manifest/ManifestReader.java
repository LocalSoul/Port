package io.github.localsoul.launcher.manifest;

import io.github.localsoul.launcher.LauncherException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Liest und prüft {@code port.toml} beider Projektarten. Fehler → Exit 2 mit Ort. */
public final class ManifestReader {

  /** Diagnose-ID für Manifestfehler. */
  public static final String ID = "PORT-MANIFEST";

  private static final String HEX64 = "[0-9a-f]{64}";
  private static final String NAME = "[a-z0-9][a-z0-9_-]*";
  private static final String RULE = "[A-Z]{2}\\d{3}";
  private static final List<String> LEVELS = List.of("error", "warn", "info");

  private ManifestReader() {}

  /**
   * Liest das Manifest eines Projektordners.
   *
   * @param projectDir Projektordner
   * @param label Dateiname für Orte, z. B. {@code port.toml} oder {@code games/a/port.toml}
   * @return Manifest
   * @throws LauncherException Exit 2, wenn die Datei fehlt oder ungültig ist
   */
  public static ProjectManifest read(Path projectDir, String label) throws LauncherException {
    Path file = projectDir.resolve(ProjectManifest.FILE);
    if (!Files.isRegularFile(file)) {
      throw new LauncherException(
          LauncherException.INVALID,
          ID,
          "kein Port-Projekt: " + ProjectManifest.FILE + " fehlt in " + projectDir,
          null,
          "port init <name> oder -C <projektordner>");
    }
    try {
      return parse(Files.readString(file, StandardCharsets.UTF_8), label);
    } catch (IOException e) {
      throw new LauncherException(
          LauncherException.GENERAL, "PORT-IO", "nicht lesbar: " + e.getMessage(), label, null);
    }
  }

  /**
   * Prüft den Text eines Manifests.
   *
   * @param text Inhalt
   * @param label Dateiname für Orte
   * @return Spiel- oder Engine-Manifest
   * @throws LauncherException Exit 2 bei Syntaxfehler, unbekannter Art, fehlendem Pflichtfeld oder
   *     ungültigem Wert
   */
  public static ProjectManifest parse(String text, String label) throws LauncherException {
    TomlFile toml = TomlFile.parse(text, label, ID);
    String kind = toml.required("project.kind");
    String name = toml.required("project.name");
    if (!name.matches(NAME)) {
      throw toml.invalidAt("project.name", "ungültiger Projektname '" + name + "'");
    }
    return switch (kind) {
      case "game" -> game(toml, name);
      case "engine" -> engine(toml, name);
      default ->
          throw toml.invalidAt(
              "project.kind", "unbekannte Projektart '" + kind + "' (game oder engine)");
    };
  }

  private static GameManifest game(TomlFile toml, String name) throws LauncherException {
    String frontend = toml.required("game.frontend");
    if (!frontend.equals("unity")) {
      throw toml.invalidAt(
          "game.frontend", "unbekanntes Frontend '" + frontend + "' (unterstützt: unity)");
    }
    String source = toml.required("game.source");
    String sha256 = toml.optional("game.sha256");
    if (sha256 != null && !sha256.matches(HEX64)) {
      throw toml.invalidAt("game.sha256", "sha256 muss 64 Hex-Zeichen in Kleinbuchstaben haben");
    }
    String adapter = toml.required("game.requires.adapter");
    String api = toml.required("game.requires.api");
    checkRange(toml, "game.requires.api", api);
    List<String> subsystems = toml.strings("game.requires.subsystems");
    LintSettings lint = lint(toml);
    return new GameManifest(name, frontend, source, sha256, adapter, api, subsystems, lint);
  }

  /** {@code [lint] disable} und {@code [lint.severity]}: Regel-IDs wie {@code SM001}. */
  private static LintSettings lint(TomlFile toml) throws LauncherException {
    toml.requireTableIfPresent("lint");
    List<String> disable = toml.strings("lint.disable");
    for (String rule : disable) {
      if (!rule.matches(RULE)) {
        throw toml.invalidAt("lint.disable", "keine Regel-ID: '" + rule + "' (z. B. SM001)");
      }
    }
    java.util.Map<String, String> severity = toml.stringTable("lint.severity");
    for (java.util.Map.Entry<String, String> entry : severity.entrySet()) {
      String key = "lint.severity." + entry.getKey();
      if (!entry.getKey().matches(RULE)) {
        throw toml.invalidAt(key, "keine Regel-ID: '" + entry.getKey() + "' (z. B. SM001)");
      }
      if (!LEVELS.contains(entry.getValue())) {
        throw toml.invalidAt(
            key, "Stufe muss error, warn oder info sein: '" + entry.getValue() + "'");
      }
    }
    return new LintSettings(disable, severity);
  }

  private static EngineManifest engine(TomlFile toml, String name) throws LauncherException {
    String adapter = toml.required("engine.provides.adapter");
    String api = toml.required("engine.provides.api");
    if (!VersionRange.isVersion(api)) {
      throw toml.invalidAt("engine.provides.api", "keine Version: '" + api + "' (z. B. 2022.3)");
    }
    String coreApi = toml.required("engine.provides.core-api");
    checkRange(toml, "engine.provides.core-api", coreApi);
    List<String> providers = toml.strings("engine.provides.providers");
    String module = toml.required("engine.build.module");
    return new EngineManifest(name, adapter, api, coreApi, providers, module);
  }

  private static void checkRange(TomlFile toml, String key, String text) throws LauncherException {
    try {
      VersionRange.parse(text);
    } catch (IllegalArgumentException e) {
      throw toml.invalidAt(key, e.getMessage());
    }
  }
}
