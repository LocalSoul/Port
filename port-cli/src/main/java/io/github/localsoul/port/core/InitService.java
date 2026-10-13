package io.github.localsoul.port.core;

import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code port init}: legt ein Spiel-Projekt an ({@code docs/cli-contract.md}). Im Monorepo unter
 * {@code games/<name>/}, sonst unter {@code <projektordner>/<name>/}. Kein Git-Init in M4
 * (Nutzerentscheidung). {@code --source} kopiert die Assembly samt der {@code *.dll} aus ihrem
 * Ordner nach {@code input/}.
 */
public final class InitService {

  /** Standardeingabe, wenn {@code --source} fehlt. */
  public static final String DEFAULT_SOURCE = "input/Assembly-CSharp.dll";

  static final String GITIGNORE = "# Port-Projekt (AGENTS.md, Regel 5)\ninput/\ngen/\n.port/\n";

  private static final List<String> DIRECTORIES = List.of("data", "input", "ir", "manual");

  private InitService() {}

  /**
   * Legt das Projekt an oder bestätigt ein identisches vorhandenes.
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
  public static CommandResult init(
      CommandContext context, String name, String frontend, Path source) throws PortException {
    checkArguments(name, frontend);
    Path root = Platform.monorepoRoot(context.projectDir());
    Path project =
        root == null ? context.projectDir().resolve(name) : root.resolve("games").resolve(name);
    List<Diagnostic> diagnostics = new ArrayList<>();
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
    Manifest manifest = Manifest.newGame(name, frontend, sourcePath, sha256);
    checkExisting(project, manifest, diagnostics);
    List<String> created = new ArrayList<>();
    try {
      for (String directory : DIRECTORIES) {
        Path dir = project.resolve(directory);
        if (!Files.isDirectory(dir)) {
          Files.createDirectories(dir);
          created.add(directory + "/");
        }
      }
      if (source != null) {
        copyInputs(source, project, created);
      }
      if (ProjectFiles.writeAtomic(project.resolve(".gitignore"), GITIGNORE)) {
        created.add(".gitignore");
      }
      if (ProjectFiles.writeAtomic(project.resolve(Manifest.FILE), manifest.toToml())) {
        created.add(Manifest.FILE);
      }
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL,
          Diagnostic.error(
              DiagnosticIds.IO, "Schreiben fehlgeschlagen: " + e.getMessage(), null, null));
    }
    if (root == null) {
      diagnostics.add(
          Diagnostic.info(
              DiagnosticIds.GIT,
              "kein Git-Repo angelegt (außerhalb des Monorepos, Git-Init kommt mit den Git-Befehlen)"));
    }
    created.sort(null);
    String base =
        root == null
            ? ProjectFiles.relative(context.projectDir(), project)
            : ProjectFiles.relative(root, project);
    JsonObject data =
        JsonObject.builder()
            .put("path", base)
            .put("monorepo", root != null)
            .put("created", JsonValue.strings(created))
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
  private static void checkExisting(Path project, Manifest manifest, List<Diagnostic> diagnostics)
      throws PortException {
    Path file = project.resolve(Manifest.FILE);
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
              Manifest.FILE,
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
