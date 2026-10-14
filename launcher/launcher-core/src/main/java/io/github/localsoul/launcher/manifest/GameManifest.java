package io.github.localsoul.launcher.manifest;

import java.util.List;
import java.util.Map;

/**
 * {@code port.toml} eines Spiels ({@code docs/cli-contract.md}, Abschnitt Manifest). Abschnitte
 * späterer Meilensteine ({@code [fix]}, {@code [lint]}, ...) werden nicht gelesen.
 *
 * @param name Projektname
 * @param frontend Frontend ({@code unity})
 * @param source Eingabe relativ zum Projekt
 * @param sha256 erwarteter SHA-256 der Eingabe oder {@code null}
 * @param requiresAdapter Name der benötigten Engine-API
 * @param requiresApi Versionsbereich der API ({@link VersionRange})
 * @param subsystems benötigte Subsystem-Arten
 * @param lint Abschnitt {@code [lint]}
 */
public record GameManifest(
    String name,
    String frontend,
    String source,
    String sha256,
    String requiresAdapter,
    String requiresApi,
    List<String> subsystems,
    LintSettings lint)
    implements ProjectManifest {

  /** Kopiert die Liste. */
  public GameManifest {
    subsystems = List.copyOf(subsystems);
    if (lint == null) {
      lint = LintSettings.NONE;
    }
  }

  /**
   * Manifest ohne {@code [lint]}.
   *
   * @param name Projektname
   * @param frontend Frontend
   * @param source Eingabe
   * @param sha256 Hash oder {@code null}
   * @param requiresAdapter benötigte API
   * @param requiresApi Versionsbereich
   * @param subsystems Subsystem-Arten
   */
  public GameManifest(
      String name,
      String frontend,
      String source,
      String sha256,
      String requiresAdapter,
      String requiresApi,
      List<String> subsystems) {
    this(
        name,
        frontend,
        source,
        sha256,
        requiresAdapter,
        requiresApi,
        subsystems,
        LintSettings.NONE);
  }

  /**
   * Manifest eines neuen Spiels.
   *
   * @param name Projektname
   * @param frontend Frontend
   * @param source Eingabe relativ zum Projekt
   * @param sha256 Hash der Eingabe oder {@code null}
   * @return Manifest mit {@code requires.adapter = frontend}, {@code api = "*"}
   */
  public static GameManifest newGame(String name, String frontend, String source, String sha256) {
    return new GameManifest(name, frontend, source, sha256, frontend, "*", List.of());
  }

  @Override
  public String kind() {
    return "game";
  }

  @Override
  public String toToml() {
    TomlText out = new TomlText();
    out.table("project").entry("kind", kind()).entry("name", name);
    out.table("game").entry("frontend", frontend).entry("source", source);
    if (sha256 != null) {
      out.entry("sha256", sha256);
    }
    out.table("game.requires").entry("adapter", requiresAdapter).entry("api", requiresApi);
    out.entry("subsystems", subsystems);
    if (!lint.isEmpty()) {
      out.table("lint").entry("disable", lint.disable());
      if (!lint.severity().isEmpty()) {
        out.table("lint.severity");
        for (Map.Entry<String, String> entry : lint.severity().entrySet()) {
          out.entry(entry.getKey(), entry.getValue());
        }
      }
    }
    return out.text();
  }
}
