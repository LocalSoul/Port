package io.github.localsoul.port.core;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.launch.GameBuilder;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Baut ein Spiel für {@code port launch run} mit der vorhandenen {@link Pipeline} ab {@code gen}
 * und merkt sich deren Diagnosen für das Ergebnis von {@code launch run}.
 */
final class PortGameBuilder implements GameBuilder {

  private final CommandContext context;
  private final List<Diagnostic> diagnostics = new ArrayList<>();

  PortGameBuilder(CommandContext context) {
    this.context = context;
  }

  List<Diagnostic> diagnostics() {
    return diagnostics;
  }

  @Override
  public void build(Path gameDir, Path engineDir) throws LauncherException {
    CommandContext gameContext =
        new CommandContext(
            context.workingDir(),
            gameDir,
            false,
            context.verbose(),
            context.quiet(),
            context.offline(),
            context.log(),
            context.platformHome(),
            context.out(),
            context.json());
    CommandResult result;
    try {
      result =
          new Pipeline()
              .run(
                  "launch run",
                  new PipelineOptions(Phase.GEN, false, engineDir, null),
                  gameContext);
    } catch (PortException e) {
      diagnostics.add(e.diagnostic());
      throw new LauncherException(
          e.exitCode().code(),
          e.diagnostic().id(),
          e.getMessage(),
          e.diagnostic().location(),
          e.diagnostic().hint());
    }
    diagnostics.addAll(result.diagnostics());
    if (result.failure() != null) {
      throw new LauncherException(
          result.failure().code(),
          DiagnosticIds.RUN,
          "Bau des Spiels fehlgeschlagen (Exit " + result.failure().code() + ")",
          ProjectFiles.relative(context.workingDir(), gameDir),
          "port -C " + gameDir + " gen");
    }
  }
}
