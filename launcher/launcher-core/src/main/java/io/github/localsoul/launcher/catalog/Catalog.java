package io.github.localsoul.launcher.catalog;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.manifest.ManifestReader;
import io.github.localsoul.launcher.manifest.ProjectManifest;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Alle Projekte des Monorepos ({@code docs/projects.md}): Spiele aus {@code games/*}, Engines aus
 * {@code engines/*} (gefunden über {@code port.toml}) und Provider aus {@code engines/*}{@code
 * /providers/*}. Sortiert nach Art (Spiel, Engine, Provider), dann Name. Defekte Unterprojekte
 * werden zu {@link Problem}s.
 *
 * @param root Wurzel des Monorepos
 * @param entries Einträge, sortiert
 * @param problems Meldungen zu defekten Unterprojekten, sortiert nach Ort
 */
public record Catalog(Path root, List<CatalogEntry> entries, List<Problem> problems) {

  /** Diagnose-ID für defekte Unterprojekte. */
  public static final String PROBLEM_ID = "PORT-CATALOG";

  private static final List<String> KINDS = List.of("game", "engine", "provider");

  /** Kopiert die Listen. */
  public Catalog {
    entries = List.copyOf(entries);
    problems = List.copyOf(problems);
  }

  /**
   * Durchsucht das Monorepo.
   *
   * @param root Wurzel
   * @return Katalog
   * @throws LauncherException Exit 1, wenn ein Ordner nicht lesbar ist
   */
  public static Catalog scan(Path root) throws LauncherException {
    List<CatalogEntry> entries = new ArrayList<>();
    List<Problem> problems = new ArrayList<>();
    try {
      for (Path dir : subdirectories(root.resolve("games"))) {
        project(root, dir, "game", entries, problems);
      }
      for (Path dir : subdirectories(root.resolve("engines"))) {
        project(root, dir, "engine", entries, problems);
        for (Path provider : subdirectories(dir.resolve("providers"))) {
          entries.add(
              new CatalogEntry(
                  "provider", provider.getFileName().toString(), relative(root, provider), null));
        }
      }
    } catch (IOException e) {
      throw new LauncherException(
          LauncherException.GENERAL,
          "PORT-IO",
          "Katalog nicht lesbar: " + e.getMessage(),
          null,
          null);
    }
    entries.sort(Catalog::compare);
    problems.sort((a, b) -> a.location().compareTo(b.location()));
    return new Catalog(root, entries, problems);
  }

  private static int compare(CatalogEntry a, CatalogEntry b) {
    int byKind = Integer.compare(KINDS.indexOf(a.kind()), KINDS.indexOf(b.kind()));
    if (byKind != 0) {
      return byKind;
    }
    int byName = a.name().compareTo(b.name());
    return byName != 0 ? byName : a.path().compareTo(b.path());
  }

  private static void project(
      Path root,
      Path dir,
      String expectedKind,
      List<CatalogEntry> entries,
      List<Problem> problems) {
    String path = relative(root, dir);
    if (!Files.isRegularFile(dir.resolve(ProjectManifest.FILE))) {
      problems.add(
          new Problem(
              PROBLEM_ID,
              "kein " + ProjectManifest.FILE + " (Submodul nicht ausgecheckt?)",
              path,
              "git submodule update --init " + path));
      return;
    }
    ProjectManifest manifest;
    try {
      manifest = ManifestReader.read(dir, path + "/" + ProjectManifest.FILE);
    } catch (LauncherException e) {
      problems.add(
          new Problem(
              PROBLEM_ID, e.getMessage(), e.location() == null ? path : e.location(), e.hint()));
      return;
    }
    if (!manifest.kind().equals(expectedKind)) {
      problems.add(
          new Problem(
              PROBLEM_ID,
              "Projektart '"
                  + manifest.kind()
                  + "' gehört nicht nach "
                  + root.relativize(dir.getParent()),
              path + "/" + ProjectManifest.FILE,
              null));
      return;
    }
    entries.add(new CatalogEntry(manifest.kind(), manifest.name(), path, manifest));
  }

  /**
   * Einträge einer Art.
   *
   * @param kind {@code game}, {@code engine}, {@code provider} oder {@code null} für alle
   * @return Einträge in Katalogreihenfolge
   */
  public List<CatalogEntry> list(String kind) {
    List<CatalogEntry> result = new ArrayList<>();
    for (CatalogEntry entry : entries) {
      if (kind == null || entry.kind().equals(kind)) {
        result.add(entry);
      }
    }
    return result;
  }

  /**
   * Sucht Einträge nach Name.
   *
   * @param name Name
   * @return Treffer (mehrere, wenn z. B. Spiel und Engine gleich heißen)
   */
  public List<CatalogEntry> find(String name) {
    List<CatalogEntry> result = new ArrayList<>();
    for (CatalogEntry entry : entries) {
      if (entry.name().equals(name)) {
        result.add(entry);
      }
    }
    return result;
  }

  /**
   * Sucht ein Spiel.
   *
   * @param name Name
   * @return Eintrag oder {@code null}
   */
  public CatalogEntry game(String name) {
    return single("game", name);
  }

  /**
   * Sucht eine Engine.
   *
   * @param name Name
   * @return Eintrag oder {@code null}
   */
  public CatalogEntry engine(String name) {
    return single("engine", name);
  }

  private CatalogEntry single(String kind, String name) {
    for (CatalogEntry entry : entries) {
      if (entry.kind().equals(kind) && entry.name().equals(name)) {
        return entry;
      }
    }
    return null;
  }

  private static List<Path> subdirectories(Path dir) throws IOException {
    List<Path> result = new ArrayList<>();
    if (!Files.isDirectory(dir)) {
      return result;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        if (Files.isDirectory(path) && !path.getFileName().toString().startsWith(".")) {
          result.add(path);
        }
      }
    }
    result.sort(null);
    return result;
  }

  private static String relative(Path root, Path path) {
    return root.relativize(path).toString().replace('\\', '/');
  }
}
