package io.github.localsoul.port.core;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Eigenes Git-Repo je Projekt (ADR-026): {@code git init}, erster Commit mit dem Gerüst und im
 * Monorepo ein Submodul mit relativer URL ({@code ./games/<name>}), nur gestaged. Nutzt das
 * Programm {@code git}; fehlt es oder scheitert ein Schritt, gibt es eine Warnung, das Projekt
 * bleibt angelegt.
 */
final class GitService {

  /** Ergebnis für {@code data.git}. */
  enum Outcome {
    CREATED("created"),
    EXISTS("exists"),
    UNAVAILABLE("unavailable"),
    FAILED("failed");

    final String spelling;

    Outcome(String spelling) {
      this.spelling = spelling;
    }
  }

  /**
   * Ergebnis.
   *
   * @param repository was mit dem Projekt-Repo geschah
   * @param submodule {@code true}, wenn das Projekt (jetzt) als Submodul eingetragen ist
   */
  record Result(Outcome repository, boolean submodule) {}

  private record Run(int exitCode, String output) {}

  private static final long TIMEOUT_SECONDS = 60;

  private GitService() {}

  /**
   * Legt das Repo an und trägt es im Monorepo ein.
   *
   * @param project Projektordner
   * @param commitMessage Nachricht des ersten Commits
   * @param monorepo Wurzel des Monorepos oder {@code null}
   * @param diagnostics sammelt Warnungen
   * @return Ergebnis
   */
  static Result setUp(
      Path project, String commitMessage, Path monorepo, List<Diagnostic> diagnostics) {
    if (!available()) {
      diagnostics.add(
          Diagnostic.warn(DiagnosticIds.GIT, "git nicht gefunden, kein Repo angelegt", null));
      return new Result(Outcome.UNAVAILABLE, false);
    }
    Outcome outcome = Outcome.EXISTS;
    if (!Files.exists(project.resolve(".git"))) {
      if (!createRepository(project, commitMessage, diagnostics)) {
        return new Result(Outcome.FAILED, false);
      }
      outcome = Outcome.CREATED;
    }
    boolean submodule = monorepo != null && registerSubmodule(monorepo, project, diagnostics);
    return new Result(outcome, submodule);
  }

  private static boolean createRepository(
      Path project, String commitMessage, List<Diagnostic> diagnostics) {
    if (!step(project, diagnostics, "git", "init", "-q", "-b", "main")) {
      return false;
    }
    if (!step(project, diagnostics, "git", "add", "-A")) {
      return false;
    }
    List<String> commit = new ArrayList<>(List.of("git"));
    if (!hasIdentity(project)) {
      commit.addAll(List.of("-c", "user.name=port", "-c", "user.email=port@localhost"));
    }
    commit.addAll(List.of("commit", "-q", "--no-verify", "-m", commitMessage));
    return step(project, diagnostics, commit.toArray(new String[0]));
  }

  /** Submodul mit relativer URL; ist der Pfad schon eingetragen, passiert nichts. */
  private static boolean registerSubmodule(
      Path monorepo, Path project, List<Diagnostic> diagnostics) {
    if (!Files.isDirectory(monorepo.resolve(".git"))
        && !Files.isRegularFile(monorepo.resolve(".git"))) {
      diagnostics.add(
          Diagnostic.warn(
              DiagnosticIds.GIT, "Monorepo ist kein Git-Repo, kein Submodul eingetragen", null));
      return false;
    }
    String path = ProjectFiles.relative(monorepo, project);
    Run registered =
        run(monorepo, "git", "config", "-f", ".gitmodules", "--get", "submodule." + path + ".path");
    if (registered.exitCode() == 0) {
      return true;
    }
    return step(
        monorepo,
        diagnostics,
        "git",
        "-c",
        "protocol.file.allow=always",
        "submodule",
        "add",
        "-q",
        "./" + path,
        path);
  }

  private static boolean hasIdentity(Path dir) {
    return run(dir, "git", "config", "user.email").exitCode() == 0
        && run(dir, "git", "config", "user.name").exitCode() == 0;
  }

  private static boolean available() {
    return run(Path.of("").toAbsolutePath(), "git", "--version").exitCode() == 0;
  }

  private static boolean step(Path dir, List<Diagnostic> diagnostics, String... command) {
    Run result = run(dir, command);
    if (result.exitCode() != 0) {
      diagnostics.add(
          Diagnostic.warn(
              DiagnosticIds.GIT,
              "Git-Schritt fehlgeschlagen: "
                  + String.join(" ", command)
                  + ": "
                  + result.output().strip(),
              null));
      return false;
    }
    return true;
  }

  private static Run run(Path dir, String... command) {
    ProcessBuilder builder = new ProcessBuilder(command).directory(dir.toFile());
    builder.redirectErrorStream(true);
    builder.environment().put("GIT_TERMINAL_PROMPT", "0");
    try {
      Process process = builder.start();
      process.getOutputStream().close();
      String output = new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
      if (!process.waitFor(TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
        process.destroyForcibly();
        return new Run(-1, "Zeitüberschreitung");
      }
      return new Run(process.exitValue(), output);
    } catch (IOException e) {
      return new Run(-1, e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return new Run(-1, "unterbrochen");
    }
  }
}
