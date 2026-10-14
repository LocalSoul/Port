package io.github.localsoul.launcher.launch;

import io.github.localsoul.launcher.LauncherException;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Prüft gebaute Module (Engine und Plattform), ohne sie zu bauen (Nutzerentscheidung M4a): {@code
 * target/classes} muss existieren und darf nicht älter sein als die neueste Datei unter {@code
 * src/main}. Sonst Exit 8 mit {@code ./mvnw -q install}.
 */
public final class PlatformCheck {

  /** Plattform-Module, die zur Laufzeit gebraucht werden, relativ zur Wurzel. */
  public static final List<String> PLATFORM_MODULES =
      List.of(
          "engine-api", "engine-core", "render-swing", "runtime-system", "launcher/launcher-boot");

  private PlatformCheck() {}

  /**
   * Liefert die Klassenordner der Module in dieser Reihenfolge.
   *
   * @param modules Modulordner
   * @return {@code target/classes} je Modul
   * @throws LauncherException Exit 8, wenn ein Modul nicht oder veraltet gebaut ist
   */
  public static List<Path> classes(List<Path> modules) throws LauncherException {
    List<Path> result = new ArrayList<>();
    for (Path module : modules) {
      Path classes = module.resolve("target/classes");
      if (!Files.isDirectory(classes)) {
        throw missing("nicht gebaut: " + module + " (target/classes fehlt)");
      }
      try {
        if (newest(module.resolve("src/main")).compareTo(newest(classes)) > 0) {
          throw missing("veraltet gebaut: " + module + " (Quellen neuer als target/classes)");
        }
      } catch (IOException e) {
        throw new LauncherException(
            LauncherException.GENERAL, "PORT-IO", e.getMessage(), module.toString(), null);
      }
      result.add(classes);
    }
    return result;
  }

  private static LauncherException missing(String message) {
    return new LauncherException(
        LauncherException.DEPENDENCY, "PORT-PLATFORM", message, null, "./mvnw -q install");
  }

  private static FileTime newest(Path dir) throws IOException {
    FileTime newest = FileTime.fromMillis(0);
    if (!Files.isDirectory(dir)) {
      return newest;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        FileTime time = Files.isDirectory(path) ? newest(path) : Files.getLastModifiedTime(path);
        if (time.compareTo(newest) > 0) {
          newest = time;
        }
      }
    }
    return newest;
  }
}
