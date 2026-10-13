package io.github.localsoul.port.frontend.unity;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/** Ersetzt das .NET-Programm: kopiert vorbereitete Rohausgabe in das {@code --out}-Verzeichnis. */
final class FakeToolRunner implements ToolRunner {

  private final List<String> resources;
  private final int exitCode;
  private final String stderr;
  final List<List<String>> commands = new ArrayList<>();

  FakeToolRunner(List<String> resources, int exitCode, String stderr) {
    this.resources = resources;
    this.exitCode = exitCode;
    this.stderr = stderr;
  }

  @Override
  public ToolResult run(List<String> command, Duration timeout) {
    commands.add(command);
    Path outDir = Path.of(command.get(command.indexOf("--out") + 1));
    for (String resource : resources) {
      copy(resource, outDir.resolve(resource));
    }
    return new ToolResult(exitCode, stderr);
  }

  private static void copy(String resource, Path target) {
    try (InputStream in = FakeToolRunner.class.getResourceAsStream("/raw/" + resource)) {
      if (in == null) {
        throw new IllegalArgumentException("Ressource fehlt: " + resource);
      }
      Files.copy(in, target);
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
