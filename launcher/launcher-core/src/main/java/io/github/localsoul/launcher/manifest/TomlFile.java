package io.github.localsoul.launcher.manifest;

import io.github.localsoul.launcher.LauncherException;
import java.util.ArrayList;
import java.util.List;
import org.tomlj.Toml;
import org.tomlj.TomlArray;
import org.tomlj.TomlParseError;
import org.tomlj.TomlParseResult;
import org.tomlj.TomlPosition;

/**
 * Gelesene TOML-Datei mit typisierten Zugriffen; jeder Fehler wird zu Exit 2 mit Ort {@code
 * datei:zeile:spalte} (bei Syntaxfehlern die Stelle, an der tomlj abbricht, sonst der Schlüssel).
 */
public final class TomlFile {

  private final TomlParseResult toml;
  private final String label;
  private final String id;

  private TomlFile(TomlParseResult toml, String label, String id) {
    this.toml = toml;
    this.label = label;
    this.id = id;
  }

  /**
   * Liest Text.
   *
   * @param text Inhalt
   * @param label Dateiname für Orte, z. B. {@code port.toml} oder {@code profiles/a.toml}
   * @param id Diagnose-ID für Fehler
   * @return Datei
   * @throws LauncherException Exit 2 bei Syntaxfehler
   */
  public static TomlFile parse(String text, String label, String id) throws LauncherException {
    TomlParseResult toml = Toml.parse(text);
    TomlFile file = new TomlFile(toml, label, id);
    if (toml.hasErrors()) {
      TomlParseError error = toml.errors().get(0);
      throw file.invalid("TOML-Syntaxfehler: " + error.getMessage(), error.position(), null);
    }
    return file;
  }

  /**
   * Pflichttext.
   *
   * @param key Schlüssel mit Punkten
   * @return Wert
   * @throws LauncherException Exit 2, wenn er fehlt oder kein Text ist
   */
  public String required(String key) throws LauncherException {
    String value = optional(key);
    if (value == null) {
      throw invalid("Pflichtfeld fehlt: " + key, null, "siehe docs/projects.md, Manifeste");
    }
    return value;
  }

  /**
   * Optionaler Text.
   *
   * @param key Schlüssel
   * @return Wert oder {@code null}
   * @throws LauncherException Exit 2, wenn er kein Text ist
   */
  public String optional(String key) throws LauncherException {
    if (!toml.contains(key)) {
      return null;
    }
    if (!toml.isString(key)) {
      throw invalid(key + " muss ein Text sein", toml.inputPositionOf(key), null);
    }
    return toml.getString(key);
  }

  /**
   * Optionale Ganzzahl.
   *
   * @param key Schlüssel
   * @param fallback Wert, wenn er fehlt
   * @return Wert
   * @throws LauncherException Exit 2, wenn er keine Ganzzahl ist
   */
  public long integer(String key, long fallback) throws LauncherException {
    if (!toml.contains(key)) {
      return fallback;
    }
    if (!toml.isLong(key)) {
      throw invalid(key + " muss eine Ganzzahl sein", toml.inputPositionOf(key), null);
    }
    return toml.getLong(key);
  }

  /**
   * Optionaler Wahrheitswert.
   *
   * @param key Schlüssel
   * @param fallback Wert, wenn er fehlt
   * @return Wert
   * @throws LauncherException Exit 2, wenn er kein Wahrheitswert ist
   */
  public boolean bool(String key, boolean fallback) throws LauncherException {
    if (!toml.contains(key)) {
      return fallback;
    }
    if (!toml.isBoolean(key)) {
      throw invalid(key + " muss true oder false sein", toml.inputPositionOf(key), null);
    }
    return toml.getBoolean(key);
  }

  /**
   * Optionale Liste von Texten.
   *
   * @param key Schlüssel
   * @return Werte, leer wenn er fehlt
   * @throws LauncherException Exit 2, wenn es keine Liste von Texten ist
   */
  public List<String> strings(String key) throws LauncherException {
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

  /**
   * Fehler zu einem Schlüssel.
   *
   * @param key Schlüssel
   * @param message Meldung
   * @return Fehler mit Ort des Schlüssels
   */
  public LauncherException invalidAt(String key, String message) {
    return invalid(message, toml.inputPositionOf(key), null);
  }

  private LauncherException invalid(String message, TomlPosition position, String hint) {
    String location = label;
    if (position != null) {
      location = location + ":" + position.line() + ":" + position.column();
    }
    return new LauncherException(LauncherException.INVALID, id, message, location, hint);
  }
}
