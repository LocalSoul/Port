package io.github.localsoul.launcher.manifest;

/** Inhalt eines {@code port.toml}: Spiel oder Engine ({@code docs/projects.md}). */
public sealed interface ProjectManifest permits GameManifest, EngineManifest {

  /** Dateiname des Manifests. */
  String FILE = "port.toml";

  /**
   * Projektname aus {@code [project] name}.
   *
   * @return Name
   */
  String name();

  /**
   * Projektart aus {@code [project] kind}.
   *
   * @return {@code game} oder {@code engine}
   */
  String kind();

  /**
   * Fester, deterministischer Text (so schreibt ihn {@code port init}).
   *
   * @return TOML-Text
   */
  String toToml();
}
