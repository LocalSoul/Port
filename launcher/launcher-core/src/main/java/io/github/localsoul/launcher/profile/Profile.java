package io.github.localsoul.launcher.profile;

import io.github.localsoul.launcher.manifest.TomlText;
import java.util.List;

/**
 * Profil {@code profiles/<name>.toml} ({@code docs/projects.md}, Abschnitt Profile): was zusammen
 * gestartet wird. {@code [run] spawn} und {@code frames} sind der Übergang bis zum Szenenformat
 * (ADR-010, M8): je Eintrag ein GameObject mit dem Skript dieses IR-Typs.
 *
 * @param name Profilname
 * @param game Spielordner relativ zur Wurzel, z. B. {@code games/demo}
 * @param engine Engine-Ordner relativ zur Wurzel, z. B. {@code engines/unity}
 * @param core Core-Implementierung ({@code engine-core})
 * @param headless ohne Fenster starten
 * @param frames Anzahl der Frames
 * @param spawn IR-Typen, die beim Start je ein GameObject bekommen
 * @param args weitere Argumente für die Startklasse der Engine
 */
public record Profile(
    String name,
    String game,
    String engine,
    String core,
    boolean headless,
    long frames,
    List<String> spawn,
    List<String> args) {

  /** Ordner der Profile relativ zur Wurzel. */
  public static final String DIR = "profiles";

  /** Einzige Core-Implementierung. */
  public static final String CORE = "engine-core";

  /** Standard für {@code frames}. */
  public static final long DEFAULT_FRAMES = 60;

  /** Kopiert die Listen. */
  public Profile {
    spawn = List.copyOf(spawn);
    args = List.copyOf(args);
  }

  /**
   * Neues Profil mit Standardwerten für {@code [run]}.
   *
   * @param name Profilname
   * @param game Spielordner
   * @param engine Engine-Ordner
   * @return Profil
   */
  public static Profile create(String name, String game, String engine) {
    return new Profile(name, game, engine, CORE, true, DEFAULT_FRAMES, List.of(), List.of());
  }

  /**
   * Datei des Profils relativ zur Wurzel.
   *
   * @return z. B. {@code profiles/demo-unity.toml}
   */
  public String path() {
    return DIR + "/" + name + ".toml";
  }

  /**
   * Fester, deterministischer Text.
   *
   * @return TOML-Text
   */
  public String toToml() {
    TomlText out = new TomlText();
    out.table("profile").entry("name", name).entry("game", game);
    out.table("engine").entry("adapter", engine).entry("core", core);
    out.table("run");
    out.entry("headless", headless)
        .entry("frames", frames)
        .entry("spawn", spawn)
        .entry("args", args);
    return out.text();
  }
}
