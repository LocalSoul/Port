package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CatalogService;
import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.PortException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParseResult;

/** {@code port catalog list [--kind game|engine|provider] [--limit n] [--cursor c]}. */
final class CatalogListCommand implements PortCommand {

  @Override
  public String name() {
    return "catalog list";
  }

  @Override
  public CommandSpec spec() {
    CommandSpec spec = CommandSpec.create().name("list");
    spec.usageMessage().description("Spiele, Engines und Provider im Monorepo.");
    spec.addOption(
        OptionSpec.builder("--kind")
            .paramLabel("<art>")
            .type(String.class)
            .description("game, engine oder provider")
            .build());
    spec.addOption(
        OptionSpec.builder("--limit")
            .paramLabel("<n>")
            .type(int.class)
            .defaultValue("0")
            .description("höchstens n Einträge (0 = alle)")
            .build());
    spec.addOption(
        OptionSpec.builder("--cursor")
            .paramLabel("<c>")
            .type(String.class)
            .description("Fortsetzung aus data.next")
            .build());
    return spec;
  }

  @Override
  public CommandResult run(ParseResult args, CommandContext context) throws PortException {
    Integer limit = args.matchedOptionValue("--limit", 0);
    return CatalogService.list(
        context,
        args.matchedOptionValue("--kind", null),
        limit,
        args.matchedOptionValue("--cursor", null));
  }
}
