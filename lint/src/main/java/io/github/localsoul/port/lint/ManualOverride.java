package io.github.localsoul.port.lint;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Markierung {@code // port:override <Symbol-ID> ir=<Hash>} in {@code manual/*.java} (ADR-027).
 *
 * @param symbol ersetztes Symbol
 * @param hash Hash der IR-S-Expression beim Anlegen (Präfix, mindestens 12 Hex-Zeichen)
 * @param location {@code manual/...java:zeile:spalte}
 */
public record ManualOverride(String symbol, String hash, String location) {

  private static final Pattern MARKER =
      Pattern.compile("//\\s*port:override\\s+(\\S+)\\s+ir=([0-9a-fA-F]+)");

  /**
   * Liest alle Markierungen einer Java-Datei.
   *
   * @param path Pfad relativ zum Projekt
   * @param text Inhalt
   * @return Markierungen in Dateireihenfolge
   */
  public static List<ManualOverride> parse(String path, String text) {
    List<ManualOverride> result = new ArrayList<>();
    String[] lines = text.split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      Matcher matcher = MARKER.matcher(lines[i]);
      if (matcher.find()) {
        result.add(
            new ManualOverride(
                matcher.group(1),
                matcher.group(2).toLowerCase(java.util.Locale.ROOT),
                path + ":" + (i + 1) + ":" + (matcher.start() + 1)));
      }
    }
    return result;
  }
}
