package io.github.localsoul.port.core;

import io.github.localsoul.launcher.manifest.CoreApi;
import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.manifest.GameManifest;
import io.github.localsoul.launcher.manifest.ProjectManifest;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code port init --kind game|engine}: legt ein Spiel- oder Engine-Projekt an ({@code
 * docs/cli-contract.md}). Im Monorepo unter {@code games/<name>/} bzw. {@code engines/<name>/},
 * sonst unter {@code <projektordner>/<name>/}. Jedes Projekt bekommt ein eigenes Git-Repo mit
 * erstem Commit, im Monorepo zusätzlich ein Submodul mit relativer URL (ADR-026). {@code --source}
 * kopiert die Assembly samt der {@code *.dll} aus ihrem Ordner nach {@code input/}.
 */
public final class InitService {

  /** Standardeingabe, wenn {@code --source} fehlt. */
  public static final String DEFAULT_SOURCE = "input/Assembly-CSharp.dll";

  static final String GITIGNORE = "# Port-Projekt (AGENTS.md, Regel 5)\ninput/\ngen/\n.port/\n";

  static final String ENGINE_GITIGNORE =
      "# Engine-Recomp (AGENTS.md, Regel 5)\ninput/\n.port/\ntarget/\n";

  private static final List<String> GAME_DIRECTORIES = List.of("data", "input", "ir", "manual");

  private static final List<String> ENGINE_DIRECTORIES = List.of("providers");

  private InitService() {}

  /**
   * Legt ein Spiel-Projekt an oder bestätigt ein identisches vorhandenes.
   *
   * @param context globale Einstellungen
   * @param name Projektname ({@code [a-z0-9][a-z0-9_-]*})
   * @param frontend Frontend ({@code unity})
   * @param source Eingabe-Assembly, die mit ihren Nachbar-DLLs nach {@code input/} kopiert wird,
   *     oder {@code null}
   * @return Ergebnis mit {@code path} und {@code created}
   * @throws PortException Exit 2 bei ungültigen Argumenten oder abweichendem Manifest, Exit 3 bei
   *     fehlender Quelle, Exit 1 bei Schreibfehlern
   */
  public static CommandResult initGame(
      CommandContext context, String name, String frontend, Path source) throws PortException {
    checkArguments(name, frontend);
    String sourcePath = DEFAULT_SOURCE;
    String sha256 = null;
    if (source != null) {
      if (!Files.isRegularFile(source)) {
        throw new PortException(
            ExitCode.INPUT,
            Diagnostic.error(DiagnosticIds.INPUT, "Quelle nicht gefunden: " + source, null, null));
      }
      sourcePath = "input/" + source.getFileName();
      sha256 = hash(source);
    }
    GameManifest manifest = GameManifest.newGame(name, frontend, sourcePath, sha256);
    return create(context, "games", manifest, GAME_DIRECTORIES, GITIGNORE, source);
  }

  /**
   * Legt ein Engine-Projekt an ({@code engines/<name>/}): Manifest mit {@code [engine.provides]}
   * und {@code [engine.build]}, Ordner {@code providers/}. Das Maven-Modul legt man selbst an.
   *
   * @param context globale Einstellungen
   * @param name Projektname, zugleich Name der bereitgestellten API
   * @return Ergebnis mit {@code path} und {@code created}
   * @throws PortException Exit 2 bei ungültigem Namen oder abweichendem Manifest
   */
  public static CommandResult initEngine(CommandContext context, String name) throws PortException {
    checkArguments(name, "unity");
    EngineManifest manifest =
        new EngineManifest(
            name,
            name,
            "0.0",
            CoreApi.defaultRange(),
            List.of(),
            "adapter-" + name,
            "io.github.localsoul.adapter." + name.replace('-', '_') + ".HeadlessMain");
    CommandResult result =
        create(context, "engines", manifest, ENGINE_DIRECTORIES, ENGINE_GITIGNORE, null);
    List<Diagnostic> diagnostics = new ArrayList<>(result.diagnostics());
    diagnostics.add(
        Diagnostic.info(
            DiagnosticIds.MANIFEST,
            "api = \"0.0\" in port.toml durch die nachgebildete Version ersetzen, Maven-Modul "
                + manifest.module()
                + " anlegen"));
    return CommandResult.ok(result.command(), result.data(), diagnostics);
  }

  private static CommandResult create(
      CommandContext context,
      String area,
      ProjectManifest manifest,
      List<String> directories,
      String gitignore,
      Path source)
      throws PortException {
    Path root = Platform.monorepoRoot(context.projectDir());
    Path project =
        root == null
            ? context.projectDir().resolve(manifest.name())
            : root.resolve(area).resolve(manifest.name());
    List<Diagnostic> diagnostics = new ArrayList<>();
    checkExisting(project, manifest, diagnostics);
    List<String> created = new ArrayList<>();
    try {
      for (String directory : directories) {
        Path dir = project.resolve(directory);
        if (!Files.isDirectory(dir)) {
          Files.createDirectories(dir);
          created.add(directory + "/");
        }
      }
      if (source != null) {
        copyInputs(source, project, created);
      }
      if (ProjectFiles.writeAtomic(project.resolve(".gitignore"), gitignore)) {
        created.add(".gitignore");
      }
      if (ProjectFiles.writeAtomic(project.resolve(ProjectManifest.FILE), manifest.toToml())) {
        created.add(ProjectManifest.FILE);
      }
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL,
          Diagnostic.error(
              DiagnosticIds.IO, "Schreiben fehlgeschlagen: " + e.getMessage(), null, null));
    }
    GitService.Result git =
        GitService.setUp(
            project,
            "chore: " + manifest.kind() + " " + manifest.name() + " angelegt (port init)",
            root,
            diagnostics);
    created.sort(null);
    String base =
        root == null
            ? ProjectFiles.relative(context.projectDir(), project)
            : ProjectFiles.relative(root, project);
    JsonObject data =
        JsonObject.builder()
            .put("path", base)
            .put("kind", manifest.kind())
            .put("monorepo", root != null)
            .put("created", JsonValue.strings(created))
            .put("git", git.repository().spelling)
            .put("submodule", git.submodule())
            .build();
    return CommandResult.ok("init", data, diagnostics);
  }

  /**
   * Kopiert die Assembly und alle {@code *.dll} aus ihrem Ordner nach {@code input/}: Das Frontend
   * löst Referenzen (bei Unity {@code Managed/UnityEngine*.dll}) im Ordner der Eingabe auf
   * (Nutzerentscheidung M4).
   */
  private static void copyInputs(Path source, Path project, List<String> created)
      throws IOException {
    List<Path> files = new ArrayList<>();
    files.add(source);
    Path folder = source.toAbsolutePath().getParent();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(folder, "*.dll")) {
      for (Path path : stream) {
        if (!path.getFileName().equals(source.getFileName()) && Files.isRegularFile(path)) {
          files.add(path);
        }
      }
    }
    files.sort(null);
    for (Path file : files) {
      String target = "input/" + file.getFileName();
      if (ProjectFiles.writeAtomic(project.resolve(target), Files.readAllBytes(file))) {
        created.add(target);
      }
    }
  }

  private static void checkArguments(String name, String frontend) throws PortException {
    if (!name.matches("[a-z0-9][a-z0-9_-]*")) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.ARGS,
              "ungültiger Projektname '" + name + "'",
              null,
              "Kleinbuchstaben, Ziffern, '-' und '_'"));
    }
    if (!frontend.equals("unity")) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.ARGS,
              "unbekanntes Frontend '" + frontend + "'",
              null,
              "--frontend unity"));
    }
  }

  /** Ein vorhandenes Manifest muss identisch sein, sonst bleibt alles unverändert (Exit 2). */
  private static void checkExisting(
      Path project, ProjectManifest manifest, List<Diagnostic> diagnostics) throws PortException {
    Path file = project.resolve(ProjectManifest.FILE);
    if (!Files.exists(file)) {
      return;
    }
    String existing;
    try {
      existing = Files.readString(file);
    } catch (IOException e) {
      existing = "";
    }
    if (!existing.equals(manifest.toToml())) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.EXISTS,
              "Projekt existiert bereits mit anderem port.toml",
              ProjectManifest.FILE,
              "anderen Namen wählen oder port.toml von Hand anpassen"));
    }
    diagnostics.add(
        Diagnostic.info(DiagnosticIds.EXISTS, "Projekt existiert bereits, unverändert"));
  }

  private static String hash(Path source) throws PortException {
    try {
      return ProjectFiles.sha256(source);
    } catch (IOException e) {
      throw new PortException(
          ExitCode.INPUT,
          Diagnostic.error(
              DiagnosticIds.INPUT, "Quelle nicht lesbar: " + e.getMessage(), null, null));
    }
  }
}
