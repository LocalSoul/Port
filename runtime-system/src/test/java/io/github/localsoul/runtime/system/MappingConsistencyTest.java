package io.github.localsoul.runtime.system;

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
import org.junit.jupiter.api.Test;

/** {@code mapping.tsv} ↔ Klassen: jedes Ziel existiert (Reflection nur im Test). */
class MappingConsistencyTest {

  private static final Set<String> FORMS =
      Set.of(
          "type",
          "method",
          "static-method",
          "getter",
          "static-getter",
          "constructor",
          "operator",
          "helper");

  private static List<String[]> rows() throws IOException {
    String text;
    try (InputStream in = MappingConsistencyTest.class.getResourceAsStream("/mapping.tsv")) {
      text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
    }
    List<String[]> rows = new ArrayList<>();
    for (String line : text.split("\n")) {
      if (!line.isBlank() && !line.startsWith("#")) {
        rows.add(line.split("\t", -1));
      }
    }
    return rows;
  }

  @Test
  void rowsAreWellFormedAndSorted() throws IOException {
    String previous = "";
    for (String[] row : rows()) {
      assertEquals(3, row.length, String.join("|", row));
      assertTrue(FORMS.contains(row[2]), "unbekannte Form " + row[2]);
      assertTrue(row[0].compareTo(previous) > 0, "nicht sortiert oder doppelt: " + row[0]);
      previous = row[0];
    }
  }

  @Test
  void everyTargetExists() throws Exception {
    Map<String, Class<?>> types = new TreeMap<>();
    for (String[] row : rows()) {
      if (row[2].equals("type")) {
        String irType = row[0].contains("/") ? row[0].substring(0, row[0].indexOf('/')) : row[0];
        types.putIfAbsent(irType, Class.forName(row[1]));
      }
    }
    for (String[] row : rows()) {
      if (row[2].equals("type")) {
        continue;
      }
      String declaring = row[0].substring(0, row[0].indexOf("::"));
      Class<?> type = types.get(declaring);
      assertTrue(type != null, "Typ ohne Mapping: " + declaring);
      if (row[2].equals("constructor")) {
        assertTrue(type.getConstructors().length > 0, row[0]);
      } else if (row[1].equals("length")) {
        assertTrue(hasMethod(String.class, "length"), row[0]);
      } else {
        assertTrue(
            hasMethod(type, row[1]), "fehlt: " + type.getName() + "." + row[1] + " für " + row[0]);
      }
    }
  }

  private static boolean hasMethod(Class<?> type, String name) {
    for (Method method : type.getMethods()) {
      if (method.getName().equals(name)) {
        return true;
      }
    }
    return false;
  }
}
