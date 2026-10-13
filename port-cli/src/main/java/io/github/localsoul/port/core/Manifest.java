package io.github.localsoul.port.core;

import java.util.List;

/**
 * Inhalt von {@code port.toml} eines Spiels ({@code docs/cli-contract.md}, Abschnitt Manifest).
 * Abschnitte späterer Meilensteine ({@code [fix]}, {@code [lint]}, ...) werden in M4 nicht gelesen.
 *
 * @param kind Projektart, in M4 nur {@code game}
 * @param name Projektname
 * @param frontend Frontend, in M4 nur {@code unity}
 * @param source Eingabe relativ zum Projekt, z. B. {@code input/Assembly-CSharp.dll}
 * @param sha256 erwarteter SHA-256 der Eingabe (hex, klein) oder {@code null}
 * @param requiresAdapter Name der benötigten Engine-API, z. B. {@code unity}
 * @param requiresApi Versionsbereich der API
 * @param subsystems benötigte Subsystem-Arten
 */
public record Manifest(
    String kind,
    String name,
    String frontend,
    String source,
    String sha256,
    String requiresAdapter,
    String requiresApi,
    List<String> subsystems) {

  /** Datei des Manifests im Projektordner. */
  public static final String FILE = "port.toml";

  /** Kopiert die Liste. */
  public Manifest {
    subsystems = List.copyOf(subsystems);
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
  public static Manifest newGame(String name, String frontend, String source, String sha256) {
    return new Manifest("game", name, frontend, source, sha256, frontend, "*", List.of());
  }

  /**
   * Fester, deterministischer Text des Manifests (so schreibt ihn {@code port init}).
   *
   * @return TOML-Text
   */
  public String toToml() {
    StringBuilder out = new StringBuilder();
    out.append("[project]\n");
    out.append("kind = ").append(quote(kind)).append('\n');
    out.append("name = ").append(quote(name)).append("\n\n");
    out.append("[game]\n");
    out.append("frontend = ").append(quote(frontend)).append('\n');
    out.append("source = ").append(quote(source)).append('\n');
    if (sha256 != null) {
      out.append("sha256 = ").append(quote(sha256)).append('\n');
    }
    out.append("\n[game.requires]\n");
    out.append("adapter = ").append(quote(requiresAdapter)).append('\n');
    out.append("api = ").append(quote(requiresApi)).append('\n');
    out.append("subsystems = [");
    for (int i = 0; i < subsystems.size(); i++) {
      out.append(i == 0 ? "" : ", ").append(quote(subsystems.get(i)));
    }
    out.append("]\n");
    return out.toString();
  }

  private static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
