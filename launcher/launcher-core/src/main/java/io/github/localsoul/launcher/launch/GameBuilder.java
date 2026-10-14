package io.github.localsoul.launcher.launch;

import io.github.localsoul.launcher.LauncherException;
import java.nio.file.Path;

/** Baut ein Spiel ({@code gen} + {@code build}); umgesetzt von {@code port-cli}. */
public interface GameBuilder {

  /**
   * Baut das Spiel nach {@code .port/classes}.
   *
   * @param gameDir Spielordner
   * @param engineDir Engine-Ordner (Adapter)
   * @throws LauncherException mit dem Exit-Code des Baus, wenn er scheitert
   */
  void build(Path gameDir, Path engineDir) throws LauncherException;
}
