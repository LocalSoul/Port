package io.github.localsoul.port.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Erkennt die Wurzel des Recomp-Monorepos ({@code docs/projects.md}). */
public final class Platform {

  private static final String MARKER = "<artifactId>recomp-parent</artifactId>";

  private Platform() {}

  /**
   * Sucht vom Ordner aufwärts die Monorepo-Wurzel: einen Ordner mit {@code pom.xml}, das das
   * Parent-POM {@code recomp-parent} definiert.
   *
   * @param start Startordner (muss nicht existieren)
   * @return Wurzel oder {@code null}
   */
  public static Path monorepoRoot(Path start) {
    Path current = start.toAbsolutePath().normalize();
    while (current != null) {
      if (isMonorepoRoot(current)) {
        return current;
      }
      current = current.getParent();
    }
    return null;
  }

  private static boolean isMonorepoRoot(Path dir) {
    Path pom = dir.resolve("pom.xml");
    if (!Files.isRegularFile(pom)) {
      return false;
    }
    try {
      String text = Files.readString(pom, StandardCharsets.UTF_8);
      int parent = text.indexOf("<parent>");
      int marker = text.indexOf(MARKER);
      return marker >= 0 && (parent < 0 || marker < parent || marker > text.indexOf("</parent>"));
    } catch (IOException e) {
      return false;
    }
  }
}
