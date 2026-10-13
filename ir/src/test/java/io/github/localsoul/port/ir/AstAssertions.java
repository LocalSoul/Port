package io.github.localsoul.port.ir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.RecordComponent;
import java.util.List;

/**
 * Struktureller Vergleich von AST-Bäumen ohne Quellpositionen.
 *
 * <p>Nur für Tests: nutzt Record-Reflection, um nicht für jede Knotenart eigenen Vergleichscode zu
 * pflegen. Komponenten vom Typ {@link SourceSpan} und der Dateiname von {@code IrFile} werden
 * übersprungen.
 */
public final class AstAssertions {

  private AstAssertions() {}

  /** Prüft strukturelle Gleichheit und meldet den Pfad zur ersten Abweichung. */
  public static void assertStructurallyEqual(Object expected, Object actual) {
    compare(expected, actual, "root");
  }

  private static void compare(Object expected, Object actual, String path) {
    if (expected == null || actual == null) {
      if (expected != actual) {
        fail(path + ": " + expected + " != " + actual);
      }
      return;
    }
    if (expected.getClass() != actual.getClass()) {
      fail(
          path
              + ": Typ "
              + expected.getClass().getSimpleName()
              + " != "
              + actual.getClass().getSimpleName());
    }
    if (expected instanceof List<?> expectedList) {
      compareLists(expectedList, (List<?>) actual, path);
    } else if (expected instanceof Record record && isAstRecord(record)) {
      compareRecords(record, (Record) actual, path);
    } else {
      assertEquals(expected, actual, path);
    }
  }

  private static boolean isAstRecord(Record record) {
    return record.getClass().getPackageName().endsWith(".ast");
  }

  private static void compareLists(List<?> expected, List<?> actual, String path) {
    assertEquals(expected.size(), actual.size(), path + ".size");
    for (int i = 0; i < expected.size(); i++) {
      compare(expected.get(i), actual.get(i), path + "[" + i + "]");
    }
  }

  private static void compareRecords(Record expected, Record actual, String path) {
    for (RecordComponent component : expected.getClass().getRecordComponents()) {
      boolean isSpan = component.getType() == SourceSpan.class;
      boolean isFileName = component.getName().equals("fileName");
      if (isSpan || isFileName) {
        continue;
      }
      Object expectedValue = read(component, expected);
      Object actualValue = read(component, actual);
      compare(expectedValue, actualValue, path + "." + component.getName());
    }
  }

  private static Object read(RecordComponent component, Record record) {
    try {
      return component.getAccessor().invoke(record);
    } catch (IllegalAccessException | InvocationTargetException e) {
      throw new AssertionError("Komponente nicht lesbar: " + component, e);
    }
  }
}
