package io.github.localsoul.port.core;

import io.github.localsoul.port.generator.java.Todo;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * Handarbeit: {@code raw}/{@code :manual}-Stellen, wie der Generator sie meldet, und die Klassen in
 * {@code manual/}. Eine Stelle ist offen, solange keine {@code manual/<Typ>.java} existiert
 * (Abgleich über den einfachen Typnamen).
 */
final class ManualWork {

  static final String DIR = "manual";

  private ManualWork() {}

  /** Member-Todo, das Handarbeit verlangt (nicht die Sammelmeldung je Klasse). */
  static boolean isManual(Todo todo) {
    String reason = todo.reason();
    return reason.startsWith("Handarbeit in manual/") || reason.contains("raw ");
  }

  /** Sammelmeldung „Klasse braucht Handarbeit“ des Generators. */
  static boolean isClassNotice(Todo todo) {
    return todo.reason().startsWith("Klasse braucht Handarbeit");
  }

  /** IR-Typname eines Todo-Symbols ({@code Typ::Member} oder {@code Typ}). */
  static String typeOf(Todo todo) {
    String symbol = todo.symbol();
    int separator = symbol.indexOf("::");
    return separator < 0 ? symbol : symbol.substring(0, separator);
  }

  /** Einfacher Name eines IR-Typs. */
  static String simpleName(String irType) {
    return irType.substring(irType.lastIndexOf('.') + 1);
  }

  /**
   * Einfache Klassennamen aller {@code *.java} unter {@code manual/}, sortiert.
   *
   * @throws IOException bei Lesefehlern
   */
  static Set<String> classNames(Path projectDir) throws IOException {
    Set<String> names = new TreeSet<>();
    Path dir = projectDir.resolve(DIR);
    if (!Files.isDirectory(dir)) {
      return names;
    }
    for (Path file : javaFiles(dir)) {
      String name = file.getFileName().toString();
      names.add(name.substring(0, name.length() - ".java".length()));
    }
    return names;
  }

  /**
   * Alle {@code *.java} unter einem Ordner, sortiert.
   *
   * @throws IOException bei Lesefehlern
   */
  static List<Path> javaFiles(Path dir) throws IOException {
    List<Path> files = new ArrayList<>();
    collect(dir, files);
    files.sort(null);
    return files;
  }

  private static void collect(Path dir, List<Path> files) throws IOException {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        if (Files.isDirectory(path)) {
          collect(path, files);
        } else if (path.getFileName().toString().endsWith(".java")) {
          files.add(path);
        }
      }
    }
  }
}
