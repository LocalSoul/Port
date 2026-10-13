package io.github.localsoul.adapter.unity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * Konsistenz von {@code mapping.tsv} und {@code adapter-metadata.json}: Jedes {@code full}/{@code
 * stub}-Symbol hat einen Mapping-Eintrag und umgekehrt; Mapping-Ziele existieren (Reflection nur im
 * Test).
 */
class MappingMetadataTest {

  private static final Set<String> FORMS =
      Set.of(
          "type",
          "method",
          "static-method",
          "getter",
          "static-getter",
          "constructor",
          "operator",
          "message");

  private static String resource(String name) throws IOException {
    try (InputStream in = MappingMetadataTest.class.getResourceAsStream("/" + name)) {
      return new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
  }

  private static List<String[]> mappingRows() throws IOException {
    List<String[]> rows = new ArrayList<>();
    for (String line : resource("mapping.tsv").split("\n")) {
      if (!line.isBlank() && !line.startsWith("#")) {
        rows.add(line.split("\t", -1));
      }
    }
    return rows;
  }

  private static Map<String, String> metadata() throws IOException {
    Map<String, String> result = new TreeMap<>();
    Matcher m =
        Pattern.compile("\\{ \"id\": \"([^\"]+)\", \"status\": \"([a-z]+)\" \\}")
            .matcher(resource("adapter-metadata.json"));
    while (m.find()) {
      result.put(m.group(1), m.group(2));
    }
    return result;
  }

  @Test
  void mappingAndMetadataCoverTheSameSymbols() throws IOException {
    Map<String, String> metadata = metadata();
    List<String> mapped = new ArrayList<>();
    for (String[] row : mappingRows()) {
      mapped.add(row[0]);
    }
    List<String> implemented = new ArrayList<>();
    for (Map.Entry<String, String> entry : metadata.entrySet()) {
      if (!entry.getValue().equals("missing")) {
        implemented.add(entry.getKey());
      }
    }
    assertEquals(implemented, mapped);
    assertEquals("stub", metadata.get("UnityEngine.Input::GetAxis#string"));
  }

  @Test
  void mappingRowsAreWellFormedAndSorted() throws IOException {
    String previous = "";
    for (String[] row : mappingRows()) {
      assertEquals(3, row.length, String.join("|", row));
      assertTrue(FORMS.contains(row[2]), "unbekannte Form " + row[2]);
      assertTrue(row[0].compareTo(previous) > 0, "nicht sortiert: " + row[0]);
      previous = row[0];
    }
  }

  @Test
  void mappingTargetsExist() throws Exception {
    Map<String, Class<?>> types = new TreeMap<>();
    for (String[] row : mappingRows()) {
      if (row[2].equals("type")) {
        types.put(row[0], Class.forName(row[1]));
      }
    }
    for (String[] row : mappingRows()) {
      if (row[2].equals("type")) {
        continue;
      }
      String declaring = row[0].substring(0, row[0].indexOf("::"));
      Class<?> type = types.get(declaring);
      assertTrue(type != null, "Typ ohne Mapping: " + declaring);
      if (row[2].equals("constructor")) {
        assertTrue(type.getConstructors().length > 0, row[0]);
        continue;
      }
      assertTrue(hasMethod(type, row[1]), "Methode fehlt: " + type.getName() + "." + row[1]);
    }
  }

  private static boolean hasMethod(Class<?> type, String name) {
    for (Method method : type.getDeclaredMethods()) {
      if (method.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }
}
