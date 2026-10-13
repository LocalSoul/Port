package io.github.localsoul.port.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlPosition;

/**
 * Liest und prüft {@code port.toml}. Jeder Fehler wird zu Exit 2 mit Ort {@code
 * port.toml:zeile:spalte} (bei Syntaxfehlern die Stelle, an der tomlj abbricht, bei falschen Werten
 * der Schlüssel).
 */
public final class ManifestReader {

  private static final String HEX64 = "[0-9a-f]{64}";

  private ManifestReader() {}

  /**
   * Liest das Manifest eines Projektordners.
   *
   * @param projectDir Projektordner
   * @return Manifest
   * @throws PortException Exit 2, wenn die Datei fehlt oder ungültig ist
   */
  public static Manifest read(Path projectDir) throws PortException {
    Path file = projectDir.resolve(Manifest.FILE);
    if (!Files.isRegularFile(file)) {
      throw invalid(
          "kein Port-Projekt: " + Manifest.FILE + " fehlt in " + projectDir,
          null,
          "port init <name> oder -C <projektordner>");
    }
    String text;
    try {
      text = Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL,
          Diagnostic.error(
              DiagnosticIds.IO, "nicht lesbar: " + e.getMessage(), Manifest.FILE, null));
    }
    return parse(text);
  }

  /**
   * Prüft den Text eines Manifests.
   *
   * @param text Inhalt von {@code port.toml}
   * @return Manifest
   * @throws PortException Exit 2 bei Syntaxfehler, fehlendem Pflichtfeld oder ungültigem Wert
   */
  public static Manifest parse(String text) throws PortException {
    TomlParseResult toml = Toml.parse(text);
    if (toml.hasErrors()) {
      TomlParseError error = toml.errors().get(0);
      throw invalid("TOML-Syntaxfehler: " + error.getMessage(), error.position(), null);
    }
    String kind = required(toml, "project.kind");
    if (!kind.equals("game")) {
      throw invalid(
          "Projektart '" + kind + "' wird in M4 nicht unterstützt (nur game, engine ab M4a)",
          toml.inputPositionOf("project.kind"),
          null);
    }
    String name = required(toml, "project.name");
    String frontend = required(toml, "game.frontend");
    if (!frontend.equals("unity")) {
      throw invalid(
          "unbekanntes Frontend '" + frontend + "' (unterstützt: unity)",
          toml.inputPositionOf("game.frontend"),
          null);
    }
    String source = required(toml, "game.source");
    String sha256 = optional(toml, "game.sha256");
    if (sha256 != null && !sha256.matches(HEX64)) {
      throw invalid(
          "sha256 muss 64 Hex-Zeichen in Kleinbuchstaben haben",
          toml.inputPositionOf("game.sha256"),
          null);
    }
    String adapter = required(toml, "game.requires.adapter");
    String api = required(toml, "game.requires.api");
    List<String> subsystems = strings(toml, "game.requires.subsystems");
    return new Manifest(kind, name, frontend, source, sha256, adapter, api, subsystems);
  }

  private static String required(TomlParseResult toml, String key) throws PortException {
    String value = optional(toml, key);
    if (value == null) {
      throw invalid("Pflichtfeld fehlt: " + key, null, "siehe docs/cli-contract.md, Manifest");
    }
    return value;
  }

  private static String optional(TomlParseResult toml, String key) throws PortException {
    if (!toml.contains(key)) {
      return null;
    }
    if (!toml.isString(key)) {
      throw invalid(key + " muss ein Text sein", toml.inputPositionOf(key), null);
    }
    return toml.getString(key);
  }

  private static List<String> strings(TomlParseResult toml, String key) throws PortException {
    List<String> values = new ArrayList<>();
    if (!toml.contains(key)) {
      return values;
    }
    TomlArray array = toml.isArray(key) ? toml.getArray(key) : null;
    if (array == null) {
      throw invalid(key + " muss eine Liste von Texten sein", toml.inputPositionOf(key), null);
    }
    for (int i = 0; i < array.size(); i++) {
      if (!(array.get(i) instanceof String value)) {
        throw invalid(key + " muss eine Liste von Texten sein", toml.inputPositionOf(key), null);
      }
      values.add(value);
    }
    return values;
  }

  private static PortException invalid(String message, TomlPosition position, String hint) {
    String location = Manifest.FILE;
    if (position != null) {
      location = location + ":" + position.line() + ":" + position.column();
    }
    return new PortException(
        ExitCode.INVALID, Diagnostic.error(DiagnosticIds.MANIFEST, message, location, hint));
  }
}
