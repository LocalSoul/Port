package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.LaunchService;
import io.github.localsoul.port.core.PortException;
import java.util.List;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

/** Befehle unter {@code port launch}. */
final class LaunchCommands {

  private LaunchCommands() {}

  static List<PortCommand> all() {
    return List.of(new ListCommand(), new NewCommand(), new CheckCommand(), new RunCommand());
  }

  static PositionalParamSpec profileName() {
    return PositionalParamSpec.builder()
        .paramLabel("<profil>")
        .arity("1")
        .type(String.class)
        .description("Profilname (profiles/<profil>.toml)")
        .build();
  }

  /** {@code port launch list}. */
  static final class ListCommand implements PortCommand {

    @Override
    public String name() {
      return "launch list";
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name("list");
      spec.usageMessage().description("Vorhandene Profile.");
      return spec;
    }

    @Override
    public CommandResult run(ParseResult args, CommandContext context) throws PortException {
      return LaunchService.list(context);
    }
  }

  /** {@code port launch new <profil> --game <g> --engine <e> [--set art=provider]}. */
  static final class NewCommand implements PortCommand {

    @Override
    public String name() {
      return "launch new";
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name("new");
      spec.usageMessage().description("Profil aus Spiel und Engine des Katalogs anlegen.");
      spec.addPositional(profileName());
      spec.addOption(
          OptionSpec.builder("--game")
              .paramLabel("<spiel>")
              .type(String.class)
              .required(true)
              .build());
      spec.addOption(
          OptionSpec.builder("--engine")
              .paramLabel("<engine>")
              .type(String.class)
              .required(true)
              .build());
      spec.addOption(
          OptionSpec.builder("--set")
              .paramLabel("<art>=<provider>")
              .type(String[].class)
              .description("Provider je Subsystem (ab M12)")
              .build());
      return spec;
    }

    @Override
    public CommandResult run(ParseResult args, CommandContext context) throws PortException {
      String[] sets = args.matchedOptionValue("--set", new String[0]);
      return LaunchService.create(
          context,
          args.matchedPositionalValue(0, ""),
          args.matchedOptionValue("--game", ""),
          args.matchedOptionValue("--engine", ""),
          List.of(sets));
    }
  }

  /** {@code port launch check <profil>}. */
  static final class CheckCommand implements PortCommand {

    @Override
    public String name() {
      return "launch check";
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name("check");
      spec.usageMessage().description("Kompatibilität von Spiel und Engine prüfen.");
      spec.addPositional(profileName());
      return spec;
    }

    @Override
    public CommandResult run(ParseResult args, CommandContext context) throws PortException {
      return LaunchService.check(context, args.matchedPositionalValue(0, ""));
    }
  }

  /** {@code port launch run <profil> [--build]}. */
  static final class RunCommand implements PortCommand {

    @Override
    public String name() {
      return "launch run";
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name("run");
      spec.usageMessage()
          .description("Bei Bedarf bauen und in eigener JVM starten (Exit-Code des Spiels).");
      spec.addPositional(profileName());
      spec.addOption(
          OptionSpec.builder("--build")
              .arity("0")
              .type(boolean.class)
              .description("Spiel neu bauen, auch wenn es aktuell ist")
              .build());
      return spec;
    }

    @Override
    public CommandResult run(ParseResult args, CommandContext context) throws PortException {
      return LaunchService.run(
          context, args.matchedPositionalValue(0, ""), args.hasMatchedOption("--build"));
    }
  }
}
