package io.github.localsoul.launcher.launch;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.catalog.Catalog;
import io.github.localsoul.launcher.catalog.CatalogEntry;
import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.profile.Profile;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code port launch run}: Spiel bei Bedarf bauen, Engine und Plattform prüfen, Klassenpfad
 * zusammensetzen ({@code docs/projects.md}, Ablauf von {@code launch run}) und die Spiel-JVM mit
 * {@code launcher-boot} starten.
 */
public final class Launcher {

  /** Startklasse der Spiel-JVM. */
  public static final String BOOT = "io.github.localsoul.launcher.boot.BootMain";

  private static final String RUNTIME_MAPPING = "runtime-system/src/main/resources/mapping.tsv";
  private static final String MODULE_MAPPING = "src/main/resources/mapping.tsv";

  private Launcher() {}

  /**
   * Bereitet den Start vor: prüft Plattform und Engine, baut das Spiel, wenn es veraltet ist oder
   * {@code forceBuild}, und setzt die Kommandozeile zusammen.
   *
   * @param catalog Katalog des Monorepos
   * @param profile geprüftes Profil
   * @param classNames voll qualifizierte Spielklassen aus {@code [run] spawn}
   * @param forceBuild {@code --build}
   * @param builder baut das Spiel
   * @return Plan
   * @throws LauncherException Exit 8 bei ungebauter Plattform, Exit des Baus bei Baufehlern
   */
  public static LaunchPlan prepare(
      Catalog catalog,
      Profile profile,
      List<String> classNames,
      boolean forceBuild,
      GameBuilder builder)
      throws LauncherException {
    Path root = catalog.root();
    Path gameDir = root.resolve(profile.game());
    Path engineDir = root.resolve(profile.engine());
    EngineManifest engine = engine(catalog, profile);
    Path module = engineDir.resolve(engine.module());
    List<Path> modules = new ArrayList<>();
    modules.add(module);
    for (String platform : PlatformCheck.PLATFORM_MODULES) {
      modules.add(root.resolve(platform));
    }
    List<Path> classpath = PlatformCheck.classes(modules);
    List<Path> mappings = List.of(module.resolve(MODULE_MAPPING), root.resolve(RUNTIME_MAPPING));
    boolean built = false;
    try {
      String hash = BuildState.hash(gameDir, mappings);
      if (forceBuild || !BuildState.upToDate(gameDir, hash)) {
        builder.build(gameDir, engineDir);
        BuildState.save(gameDir, hash);
        built = true;
      }
    } catch (IOException e) {
      throw new LauncherException(
          LauncherException.GENERAL, "PORT-IO", e.getMessage(), profile.game(), null);
    }
    classpath.add(0, gameDir.resolve(BuildState.CLASSES));
    return new LaunchPlan(command(classpath, engine, profile, classNames), built);
  }

  private static EngineManifest engine(Catalog catalog, Profile profile) throws LauncherException {
    for (CatalogEntry entry : catalog.list("engine")) {
      if (entry.path().equals(profile.engine())
          && entry.manifest() instanceof EngineManifest engine) {
        return engine;
      }
    }
    throw new LauncherException(
        LauncherException.INVALID,
        "PORT-PROFILE",
        "Engine nicht gefunden: " + profile.engine(),
        profile.path(),
        "port launch check " + profile.name());
  }

  private static List<String> command(
      List<Path> classpath, EngineManifest engine, Profile profile, List<String> classNames) {
    StringBuilder path = new StringBuilder();
    for (Path entry : classpath) {
      path.append(path.isEmpty() ? "" : File.pathSeparator).append(entry);
    }
    List<String> command = new ArrayList<>();
    command.add(Path.of(System.getProperty("java.home"), "bin", "java").toString());
    command.add("-cp");
    command.add(path.toString());
    command.add(BOOT);
    command.add("--adapter");
    command.add(engine.adapter());
    command.add("--frames");
    command.add(Long.toString(profile.frames()));
    command.addAll(profile.args());
    command.add("--");
    command.addAll(classNames);
    return command;
  }

  /**
   * Startet die Spiel-JVM und reicht ihre Ausgabe durch.
   *
   * @param plan Plan
   * @param workingDir Arbeitsverzeichnis des Spiels
   * @param out Ziel für stdout des Spiels
   * @param err Ziel für stderr des Spiels
   * @return Exit-Code des Spiels
   * @throws LauncherException Exit 1, wenn der Prozess nicht startet
   */
  public static int run(LaunchPlan plan, Path workingDir, PrintStream out, PrintStream err)
      throws LauncherException {
    ProcessBuilder builder = new ProcessBuilder(plan.command()).directory(workingDir.toFile());
    try {
      Process process = builder.start();
      process.getOutputStream().close();
      Thread errors = pump(process.getErrorStream(), err);
      copy(process.getInputStream(), out);
      int code = process.waitFor();
      errors.join();
      out.flush();
      err.flush();
      return code;
    } catch (IOException e) {
      throw new LauncherException(
          LauncherException.GENERAL,
          "PORT-RUN",
          "Start fehlgeschlagen: " + e.getMessage(),
          null,
          null);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new LauncherException(
          LauncherException.GENERAL, "PORT-RUN", "unterbrochen", null, null);
    }
  }

  private static Thread pump(InputStream in, OutputStream target) {
    Thread thread =
        new Thread(
            () -> {
              try {
                copy(in, target);
              } catch (IOException e) {
                // Ausgabe des Spiels ist nicht mehr lesbar; der Exit-Code zählt.
              }
            },
            "launch-stderr");
    thread.start();
    return thread;
  }

  private static void copy(InputStream in, OutputStream target) throws IOException {
    byte[] buffer = new byte[8192];
    int read = in.read(buffer);
    while (read >= 0) {
      target.write(buffer, 0, read);
      read = in.read(buffer);
    }
  }
}
