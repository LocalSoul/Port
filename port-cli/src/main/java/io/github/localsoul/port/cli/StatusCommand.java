package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.PortException;
import io.github.localsoul.port.core.StatusService;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParseResult;

/** {@code port status}. */
final class StatusCommand implements PortCommand {

  @Override
  public String name() {
    return "status";
  }

  @Override
  public CommandSpec spec() {
    CommandSpec spec = CommandSpec.create().name(name());
    spec.usageMessage().description("Phase, Klassen, offene Handarbeit, geänderte IR-Knoten.");
    return spec;
  }

  @Override
  public CommandResult run(ParseResult args, CommandContext context) throws PortException {
    return StatusService.status(context);
  }
}
