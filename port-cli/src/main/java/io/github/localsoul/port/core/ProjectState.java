package io.github.localsoul.port.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/** Zuletzt abgeschlossene Phase in {@code .port/state.tsv} ({@code phase<TAB>name}). */
public final class ProjectState {

  static final String FILE = Project.CACHE + "/state.tsv";

  /** Phase eines frisch angelegten Projekts. */
  public static final String INITIAL = "init";

  private ProjectState() {}

  /**
   * Liest die zuletzt abgeschlossene Phase.
   *
   * @param projectDir Projektordner
   * @return Phasenname oder {@link #INITIAL}
   */
  public static String phase(Path projectDir) {
    Path file = projectDir.resolve(FILE);
    if (!Files.isRegularFile(file)) {
      return INITIAL;
    }
    try {
      for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
        if (line.startsWith("phase\t")) {
          return line.substring("phase\t".length());
        }
      }
    } catch (IOException e) {
      return INITIAL;
    }
    return INITIAL;
  }

  static void complete(Path projectDir, Phase phase) throws IOException {
    ProjectFiles.writeAtomic(projectDir.resolve(FILE), "phase\t" + phase.spelling() + "\n");
  }
}
