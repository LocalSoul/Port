package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.DiagnosticIds;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.core.Phase;
import io.github.localsoul.port.core.Pipeline;
import io.github.localsoul.port.core.PipelineOptions;
import io.github.localsoul.port.core.PortException;
import java.nio.file.Path;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

/**
 * Lifecycle-Befehl einer Phase ({@code extract}, {@code ir}, ...): führt die Phase und alle
 * Folgephasen aus; {@code --only} nur diese, {@code --from <phase>} ab einer anderen Phase.
 */
final class LifecycleCommand implements PortCommand {

  private final Phase phase;
  private final String description;

  LifecycleCommand(Phase phase, String description) {
    this.phase = phase;
    this.description = description;
  }

  @Override
  public String name() {
    return phase.spelling();
  }

  @Override
  public CommandSpec spec() {
    CommandSpec spec = CommandSpec.create().name(name());
    spec.usageMessage().description(description + " Folgephasen laufen mit.");
    spec.addOption(
        OptionSpec.builder("--only")
            .arity("0")
            .type(boolean.class)
            .description("nur diese Phase, keine Folgephasen")
            .build());
    spec.addOption(
        OptionSpec.builder("--from")
            .paramLabel("<phase>")
            .type(String.class)
            .description("ab dieser Phase statt ab " + name())
            .build());
    spec.addOption(
        OptionSpec.builder("--adapter")
            .paramLabel("<pfad>")
            .type(String.class)
            .description(
                "Adapter-Ordner für gen/build (Standard: engines/<adapter>/adapter-<adapter>)")
            .build());
    if (phase == Phase.INDEX) {
      spec.addOption(
          OptionSpec.builder("--rebuild")
              .arity("0")
              .type(boolean.class)
              .description("Index verwerfen und neu aufbauen")
              .build());
    }
    if (phase == Phase.LINT) {
      spec.addOption(
          OptionSpec.builder("--rule")
              .paramLabel("<id>")
              .type(String[].class)
              .description("nur diese Regel(n), z. B. SH001")
              .build());
      spec.addOption(
          OptionSpec.builder("--class")
              .paramLabel("<muster>")
              .type(String.class)
              .description("nur Befunde zu Typen, deren Name dem Muster entspricht (*, ?)")
              .build());
      spec.addOption(
          OptionSpec.builder("--fix-hint")
              .arity("0")
              .type(boolean.class)
              .description("Hinweise zur Behebung ausgeben")
              .build());
    }
    if (phase == Phase.GEN) {
      spec.addOption(
          OptionSpec.builder("--class")
              .paramLabel("<muster>")
              .type(String.class)
              .description("nur Typen, deren Name dem Muster entspricht (*, ?)")
              .build());
    }
    return spec;
  }

  private PipelineOptions.LintOptions lintOptions(ParseResult args) {
    if (phase != Phase.LINT) {
      return PipelineOptions.LintOptions.DEFAULT;
    }
    String[] rules = args.matchedOptionValue("--rule", new String[0]);
    return new PipelineOptions.LintOptions(
        java.util.List.of(rules),
        args.matchedOptionValue("--class", null),
        args.hasMatchedOption("--fix-hint"));
  }

  @Override
  public CommandResult run(ParseResult args, CommandContext context) throws PortException {
    Phase from = phase;
    String fromText = args.matchedOptionValue("--from", null);
    if (fromText != null) {
      from = Phase.fromSpelling(fromText);
      if (from == null) {
        throw new PortException(
            ExitCode.INVALID,
            Diagnostic.error(
                DiagnosticIds.ARGS,
                "unbekannte Phase '" + fromText + "'",
                null,
                "extract, ir, index, lint, gen oder build"));
      }
    }
    String adapter = args.matchedOptionValue("--adapter", null);
    Path adapterPath = adapter == null ? null : context.workingDir().resolve(adapter).normalize();
    String classFilter = phase == Phase.GEN ? args.matchedOptionValue("--class", null) : null;
    PipelineOptions options =
        new PipelineOptions(
            from,
            args.hasMatchedOption("--only"),
            adapterPath,
            classFilter,
            phase == Phase.INDEX && args.hasMatchedOption("--rebuild"),
            lintOptions(args));
    return new Pipeline().run(name(), options, context);
  }
}
