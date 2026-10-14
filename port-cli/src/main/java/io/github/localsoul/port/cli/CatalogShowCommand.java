package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CatalogService;
import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.PortException;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

/** {@code port catalog show <name> [--kind game|engine|provider]}. */
final class CatalogShowCommand implements PortCommand {

  @Override
  public String name() {
    return "catalog show";
  }

  @Override
  public CommandSpec spec() {
    CommandSpec spec = CommandSpec.create().name("show");
    spec.usageMessage().description("Details, Anforderungen und Abdeckung eines Eintrags.");
    spec.addPositional(
        PositionalParamSpec.builder().paramLabel("<name>").arity("1").type(String.class).build());
    spec.addOption(
        OptionSpec.builder("--kind")
            .paramLabel("<art>")
            .type(String.class)
            .description("bei gleichen Namen")
            .build());
    return spec;
  }

  @Override
  public CommandResult run(ParseResult args, CommandContext context) throws PortException {
    return CatalogService.show(
        context, args.matchedPositionalValue(0, ""), args.matchedOptionValue("--kind", null));
  }
}
