package io.github.localsoul.port.cli;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;

/** Ruft die CLI im Test auf und sammelt stdout, stderr und Exit-Code. */
final class Cli {

  record Run(int exitCode, String out, String err) {}

  private Cli() {}

  /** Plattform = dieses Repository (wie {@code PORT_HOME}). */
  static Run run(Path workingDir, String... args) {
    return run(Commands.all(), Golden.ROOT, workingDir, args);
  }

  static Run run(List<PortCommand> commands, Path workingDir, String... args) {
    return run(commands, Golden.ROOT, workingDir, args);
  }

  static Run run(List<PortCommand> commands, Path home, Path workingDir, String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    int exitCode =
        new Main(commands, home)
            .run(
                args,
                new PrintStream(out, true, StandardCharsets.UTF_8),
                new PrintStream(err, true, StandardCharsets.UTF_8),
                workingDir);
    return new Run(
        exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }
}
