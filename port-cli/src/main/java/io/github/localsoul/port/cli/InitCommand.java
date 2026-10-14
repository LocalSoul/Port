package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.DiagnosticIds;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.core.InitService;
import io.github.localsoul.port.core.PortException;
import java.nio.file.Path;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

/** {@code port init <name> [--kind game|engine] [--frontend unity] [--source <pfad>]}. */
final class InitCommand implements PortCommand {

  @Override
  public String name() {
    return "init";
  }

  @Override
  public CommandSpec spec() {
    CommandSpec spec = CommandSpec.create().name(name());
    spec.usageMessage()
        .description("Spiel-Projekt mit port.toml anlegen (im Monorepo unter games/).");
    spec.addPositional(
        PositionalParamSpec.builder()
            .paramLabel("<name>")
            .arity("1")
            .type(String.class)
            .description("Projektname")
            .build());
    spec.addOption(
        OptionSpec.builder("--kind")
            .paramLabel("<art>")
            .type(String.class)
            .defaultValue("game")
            .description("game (Standard, games/<name>/) oder engine (engines/<name>/)")
            .build());
    spec.addOption(
        OptionSpec.builder("--frontend")
            .paramLabel("<frontend>")
            .type(String.class)
            .defaultValue("unity")
            .description("Frontend (Standard: unity)")
            .build());
    spec.addOption(
        OptionSpec.builder("--source")
            .paramLabel("<pfad>")
            .type(String.class)
            .description("Assembly, die nach input/ kopiert wird (Hash kommt ins Manifest)")
            .build());
    return spec;
  }

  @Override
  public CommandResult run(ParseResult args, CommandContext context) throws PortException {
    String name = args.matchedPositionalValue(0, "");
    String frontend = args.matchedOptionValue("--frontend", "unity");
    String source = args.matchedOptionValue("--source", null);
    String kind = args.matchedOptionValue("--kind", "game");
    if (kind.equals("engine")) {
      return InitService.initEngine(context, name);
    }
    if (!kind.equals("game")) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.ARGS,
              "unbekannte Projektart '" + kind + "'",
              null,
              "--kind game oder --kind engine"));
    }
    Path sourcePath = source == null ? null : context.workingDir().resolve(source);
    return InitService.initGame(context, name, frontend, sourcePath);
  }
}
