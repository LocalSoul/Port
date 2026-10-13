package io.github.localsoul.port.generator.java;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Abbildung von IR-Symbolen auf Java ({@code mapping.tsv} eines Adapters oder von {@code
 * runtime-system}). Format: je Zeile IR-Symbol, Java-Symbol, Form, getrennt durch Tabulatoren;
 * Zeilen mit {@code #} und Leerzeilen werden übersprungen.
 */
public final class Mapping {

  private final Map<String, MappingEntry> entries;

  private Mapping(Map<String, MappingEntry> entries) {
    this.entries = entries;
  }

  /**
   * Leeres Mapping.
   *
   * @return Mapping ohne Einträge
   */
  public static Mapping empty() {
    return new Mapping(new TreeMap<>());
  }

  /**
   * Liest ein Mapping aus Text.
   *
   * @param tsv Inhalt von {@code mapping.tsv}
   * @return Mapping
   * @throws MappingException bei falscher Spaltenzahl, unbekannter Form oder doppeltem Symbol
   */
  public static Mapping parse(String tsv) throws MappingException {
    Map<String, MappingEntry> entries = new TreeMap<>();
    String[] lines = tsv.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      String line = lines[i].strip();
      if (line.isEmpty() || line.startsWith("#")) {
        continue;
      }
      MappingEntry entry = parseLine(line, i + 1);
      if (entries.put(entry.irSymbol(), entry) != null) {
        throw new MappingException("Zeile " + (i + 1) + ": Symbol doppelt: " + entry.irSymbol());
      }
    }
    return new Mapping(entries);
  }

  private static MappingEntry parseLine(String line, int number) throws MappingException {
    String[] columns = line.split("\t", -1);
    if (columns.length != 3) {
      throw new MappingException(
          "Zeile " + number + ": 3 Spalten erwartet, gefunden " + columns.length);
    }
    MappingForm form = MappingForm.fromSpelling(columns[2]);
    if (form == null) {
      throw new MappingException("Zeile " + number + ": unbekannte Form '" + columns[2] + "'");
    }
    return new MappingEntry(columns[0], columns[1], form);
  }

  /**
   * Liest eine Datei.
   *
   * @param path Pfad zu {@code mapping.tsv}
   * @return Mapping
   * @throws IOException bei Lesefehlern
   * @throws MappingException bei ungültigem Inhalt
   */
  public static Mapping load(Path path) throws IOException, MappingException {
    return parse(Files.readString(path, StandardCharsets.UTF_8));
  }

  /**
   * Fügt zwei Mappings zusammen (z. B. Adapter und {@code runtime-system}).
   *
   * @param other weiteres Mapping
   * @return neues Mapping
   * @throws MappingException wenn ein Symbol in beiden vorkommt
   */
  public Mapping merge(Mapping other) throws MappingException {
    Map<String, MappingEntry> merged = new TreeMap<>(entries);
    for (MappingEntry entry : other.entries.values()) {
      if (merged.put(entry.irSymbol(), entry) != null) {
        throw new MappingException("Symbol in beiden Mappings: " + entry.irSymbol());
      }
    }
    return new Mapping(merged);
  }

  /**
   * Sucht den Eintrag zu einer Symbol-ID.
   *
   * @param symbolId Symbol-ID (ADR-025)
   * @return Eintrag oder {@code null}
   */
  public MappingEntry entry(String symbolId) {
    return entries.get(symbolId);
  }

  /**
   * Liefert den Java-Klassennamen eines IR-Typs.
   *
   * @param irType qualifizierter IR-Typname
   * @return voll qualifizierter Java-Name oder {@code null}
   */
  public String javaType(String irType) {
    MappingEntry entry = entries.get(irType);
    if (entry == null || entry.form() != MappingForm.TYPE) {
      return null;
    }
    return entry.javaSymbol();
  }

  /**
   * Liefert die Lifecycle-Nachrichten eines Typs, z. B. {@code Update#} für {@code
   * UnityEngine.MonoBehaviour}.
   *
   * @param irType qualifizierter IR-Typname
   * @return Signaturteile nach {@code ::} mit Java-Name, sortiert
   */
  public List<MappingEntry> messagesOf(String irType) {
    List<MappingEntry> result = new ArrayList<>();
    String prefix = irType + "::";
    for (MappingEntry entry : entries.values()) {
      if (entry.form() == MappingForm.MESSAGE && entry.irSymbol().startsWith(prefix)) {
        result.add(entry);
      }
    }
    return result;
  }
}
