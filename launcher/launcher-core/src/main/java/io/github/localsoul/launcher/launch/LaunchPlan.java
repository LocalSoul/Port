package io.github.localsoul.launcher.launch;

import java.util.List;

/**
 * Was {@code port launch run} startet.
 *
 * @param command vollständige Kommandozeile der Spiel-JVM
 * @param built {@code true}, wenn das Spiel dafür (neu) gebaut wurde
 */
public record LaunchPlan(List<String> command, boolean built) {

  /** Kopiert die Liste. */
  public LaunchPlan {
    command = List.copyOf(command);
  }
}
