package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Golden Files unter {@code testdata/golden/cli/}; Aktualisieren nur mit {@code
 * -Dgolden.update=true}.
 */
public final class Golden {

  /** Wurzel des Repositorys. */
  public static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

  private static final Path DIR = ROOT.resolve("testdata/golden/cli");

  private Golden() {}

  /**
   * Vergleicht mit einer Golden-Datei oder schreibt sie neu.
   *
   * @param name Dateiname
   * @param actual Inhalt
   * @throws IOException bei Lesefehlern
   */
  public static void assertMatches(String name, String actual) throws IOException {
    Path file = DIR.resolve(name);
    if (Boolean.getBoolean("golden.update")) {
      Files.createDirectories(DIR);
      Files.writeString(file, actual, StandardCharsets.UTF_8);
    }
    assertTrue(Files.exists(file), "Golden File fehlt, mit -Dgolden.update=true erzeugen: " + file);
    assertEquals(Files.readString(file, StandardCharsets.UTF_8), actual, name);
  }
}
