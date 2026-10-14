package io.github.localsoul.launcher.catalog;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * API-Abdeckung einer Engine aus {@code adapter-metadata.json} ihres Moduls: Anzahl der Symbole mit
 * Status {@code full} und {@code stub}. Liest nur die Statusfelder (kein JSON-Parser nötig).
 *
 * @param full vollständig umgesetzte Symbole
 * @param stub nur als Stub vorhandene Symbole
 */
public record Coverage(int full, int stub) {

  /** Metadaten relativ zum Modulordner. */
  public static final String METADATA = "src/main/resources/adapter-metadata.json";

  private static final Pattern STATUS = Pattern.compile("\"status\"\\s*:\\s*\"(full|stub)\"");

  /**
   * Liest die Abdeckung.
   *
   * @param module Modulordner der Engine
   * @return Abdeckung oder {@code null}, wenn es keine Metadaten gibt
   */
  public static Coverage of(Path module) {
    Path file = module.resolve(METADATA);
    if (!Files.isRegularFile(file)) {
      return null;
    }
    String text;
    try {
      text = Files.readString(file, StandardCharsets.UTF_8);
    } catch (IOException e) {
      return null;
    }
    int full = 0;
    int stub = 0;
    Matcher matcher = STATUS.matcher(text);
    while (matcher.find()) {
      if (matcher.group(1).equals("full")) {
        full++;
      } else {
        stub++;
      }
    }
    return new Coverage(full, stub);
  }
}
