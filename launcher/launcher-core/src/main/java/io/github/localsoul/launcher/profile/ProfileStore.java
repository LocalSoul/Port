package io.github.localsoul.launcher.profile;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.manifest.TomlFile;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Liest und listet Profile in {@code profiles/}. Fehler → Exit 2 mit Ort und nächstem Schritt. */
public final class ProfileStore {

  /** Diagnose-ID für Profilfehler. */
  public static final String ID = "PORT-PROFILE";

  private static final String NAME = "[a-z0-9][a-z0-9_-]*";

  private ProfileStore() {}

  /**
   * Prüft einen Profilnamen.
   *
   * @param name Name
   * @throws LauncherException Exit 2 bei ungültigem Namen
   */
  public static void checkName(String name) throws LauncherException {
    if (!name.matches(NAME)) {
      throw new LauncherException(
          LauncherException.INVALID,
          ID,
          "ungültiger Profilname '" + name + "'",
          null,
          "Kleinbuchstaben, Ziffern, '-' und '_'");
    }
  }

  /**
   * Namen aller Profile, sortiert.
   *
   * @param root Wurzel des Monorepos
   * @return Namen ohne {@code .toml}
   * @throws LauncherException Exit 1 bei Lesefehlern
   */
  public static List<String> names(Path root) throws LauncherException {
    List<String> names = new ArrayList<>();
    Path dir = root.resolve(Profile.DIR);
    if (!Files.isDirectory(dir)) {
      return names;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.toml")) {
      for (Path path : stream) {
        String file = path.getFileName().toString();
        names.add(file.substring(0, file.length() - ".toml".length()));
      }
    } catch (IOException e) {
      throw new LauncherException(
          LauncherException.GENERAL, "PORT-IO", e.getMessage(), Profile.DIR, null);
    }
    names.sort(null);
    return names;
  }

  /**
   * Liest ein Profil.
   *
   * @param root Wurzel
   * @param name Profilname
   * @return Profil
   * @throws LauncherException Exit 2, wenn es fehlt oder ungültig ist
   */
  public static Profile read(Path root, String name) throws LauncherException {
    checkName(name);
    String label = Profile.DIR + "/" + name + ".toml";
    Path file = root.resolve(label);
    if (!Files.isRegularFile(file)) {
      throw new LauncherException(
          LauncherException.INVALID,
          ID,
          "Profil nicht gefunden: " + name,
          label,
          "port launch list oder port launch new " + name + " --game <spiel> --engine <engine>");
    }
    try {
      return parse(Files.readString(file, StandardCharsets.UTF_8), label);
    } catch (IOException e) {
      throw new LauncherException(
          LauncherException.GENERAL, "PORT-IO", e.getMessage(), label, null);
    }
  }

  /**
   * Prüft den Text eines Profils.
   *
   * @param text Inhalt
   * @param label Dateiname für Orte
   * @return Profil
   * @throws LauncherException Exit 2 bei Syntaxfehler oder ungültigem Feld
   */
  public static Profile parse(String text, String label) throws LauncherException {
    TomlFile toml = TomlFile.parse(text, label, ID);
    String name = toml.required("profile.name");
    String game = toml.required("profile.game");
    String engine = toml.required("engine.adapter");
    String core = toml.optional("engine.core");
    if (core == null) {
      core = Profile.CORE;
    }
    boolean headless = toml.bool("run.headless", true);
    long frames = toml.integer("run.frames", Profile.DEFAULT_FRAMES);
    if (frames < 0) {
      throw toml.invalidAt("run.frames", "run.frames muss >= 0 sein");
    }
    List<String> spawn = toml.strings("run.spawn");
    List<String> args = toml.strings("run.args");
    return new Profile(name, game, engine, core, headless, frames, spawn, args);
  }
}
