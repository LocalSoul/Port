package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.DiagnosticIds;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.core.ExitCodes;
import io.github.localsoul.port.core.PortException;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.output.JsonRenderer;
import io.github.localsoul.port.output.TextRenderer;
import java.io.PrintStream;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.ParseResult;
import picocli.CommandLine.ScopeType;

/**
 * Einstieg der CLI {@code port} ({@code docs/cli-contract.md}). Liest die Argumente mit dem
 * programmatischen Modell von picocli (keine Annotationen, keine Reflection im eigenen Code), führt
 * genau einen Unterbefehl aus und gibt dessen Ergebnis als Text oder JSON-Hülle aus.
 */
public final class Main {

  static final String JSON = "--json";
  static final String DIRECTORY = "-C";
  static final String QUIET = "-q";
  static final String VERBOSE = "-v";
  static final String OFFLINE = "--offline";
  static final String STRICT = "--strict";

  private final List<PortCommand> commands;

  Main(List<PortCommand> commands) {
    this.commands = List.copyOf(commands);
  }

  /**
   * Startet die CLI und beendet die JVM mit dem Exit-Code.
   *
   * @param args Kommandozeile
   */
  public static void main(String[] args) {
    Path workingDir = Path.of("").toAbsolutePath();
    int exitCode = new Main(Commands.all()).run(args, System.out, System.err, workingDir);
    System.exit(exitCode);
  }

  /**
   * Führt einen Aufruf aus, ohne die JVM zu beenden (für Tests).
   *
   * @param args Kommandozeile
   * @param out stdout
   * @param err stderr
   * @param workingDir Arbeitsverzeichnis (Basis für {@code -C})
   * @return Exit-Code
   */
  public int run(String[] args, PrintStream out, PrintStream err, Path workingDir) {
    CommandSpec root = rootSpec();
    CommandLine commandLine = new CommandLine(root);
    ParseResult parsed;
    try {
      parsed = commandLine.parseArgs(args);
    } catch (ParameterException e) {
      return failArgs(commandName(e), e.getMessage(), contains(args, JSON), out, err);
    }
    if (parsed.isUsageHelpRequested()) {
      commandLine.usage(out);
      return ExitCode.SUCCESS.code();
    }
    ParseResult sub = parsed.subcommand();
    if (sub == null) {
      return failArgs("port", "Befehl fehlt", flag(parsed, null, JSON), out, err);
    }
    if (sub.isUsageHelpRequested()) {
      sub.commandSpec().commandLine().usage(out);
      return ExitCode.SUCCESS.code();
    }
    boolean json = flag(parsed, sub, JSON);
    CommandContext context = context(parsed, sub, workingDir, err);
    PortCommand command = command(sub.commandSpec().name());
    CommandResult result;
    try {
      result = command.run(sub, context);
    } catch (PortException e) {
      result =
          CommandResult.failed(
              command.name(), JsonObject.empty(), List.of(e.diagnostic()), e.exitCode());
    }
    return emit(result, json, context.strict(), context.quiet(), out, err);
  }

  private CommandSpec rootSpec() {
    CommandSpec root = CommandSpec.create().name("port");
    root.usageMessage().description("Headless CLI der Recomp-Toolchain (docs/cli-contract.md).");
    root.addOption(
        OptionSpec.builder("-h", "--help")
            .usageHelp(true)
            .scopeType(ScopeType.INHERIT)
            .description("Hilfe anzeigen")
            .build());
    root.addOption(flag(JSON, "maschinenlesbare Ausgabe, genau ein JSON-Objekt auf stdout"));
    root.addOption(
        OptionSpec.builder(DIRECTORY)
            .paramLabel("<dir>")
            .type(String.class)
            .scopeType(ScopeType.INHERIT)
            .description("Projektordner (Standard: aktuelles Verzeichnis)")
            .build());
    root.addOption(flag(QUIET, "leise: nur Fehler"));
    root.addOption(flag(VERBOSE, "ausführlich: Fortschritt auf stderr"));
    root.addOption(flag(OFFLINE, "keine Netzwerkzugriffe (in M4 ohne Wirkung)"));
    root.addOption(flag(STRICT, "offene Handarbeit als Fehler (Exit 5)"));
    for (PortCommand command : commands) {
      root.addSubcommand(command.name(), command.spec());
    }
    return root;
  }

  private static OptionSpec flag(String name, String description) {
    return OptionSpec.builder(name)
        .arity("0")
        .type(boolean.class)
        .scopeType(ScopeType.INHERIT)
        .description(description)
        .build();
  }

  /** Globale Flags dürfen vor oder nach dem Unterbefehl stehen. */
  private static boolean flag(ParseResult root, ParseResult sub, String name) {
    if (root.hasMatchedOption(name)) {
      return true;
    }
    return sub != null && sub.hasMatchedOption(name);
  }

  private static CommandContext context(
      ParseResult root, ParseResult sub, Path workingDir, PrintStream err) {
    String directory = sub.matchedOptionValue(DIRECTORY, null);
    if (directory == null) {
      directory = root.matchedOptionValue(DIRECTORY, null);
    }
    Path projectDir = directory == null ? workingDir : workingDir.resolve(directory);
    return new CommandContext(
        workingDir,
        projectDir.toAbsolutePath().normalize(),
        flag(root, sub, STRICT),
        flag(root, sub, VERBOSE),
        flag(root, sub, QUIET),
        flag(root, sub, OFFLINE),
        err);
  }

  private PortCommand command(String name) {
    for (PortCommand command : commands) {
      if (command.name().equals(name)) {
        return command;
      }
    }
    throw new IllegalStateException("Befehl ohne Implementierung: " + name);
  }

  private static String commandName(ParameterException e) {
    CommandSpec spec = e.getCommandLine().getCommandSpec();
    return spec.parent() == null ? "port" : spec.name();
  }

  private static boolean contains(String[] args, String name) {
    for (String arg : args) {
      if (arg.equals(name)) {
        return true;
      }
    }
    return false;
  }

  private static int failArgs(
      String command, String message, boolean json, PrintStream out, PrintStream err) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    diagnostics.add(Diagnostic.error(DiagnosticIds.ARGS, message, null, "port --help"));
    CommandResult result =
        CommandResult.failed(command, JsonObject.empty(), diagnostics, ExitCode.INVALID);
    return emit(result, json, false, false, out, err);
  }

  private static int emit(
      CommandResult result,
      boolean json,
      boolean strict,
      boolean quiet,
      PrintStream out,
      PrintStream err) {
    ExitCode exitCode = ExitCodes.of(result, strict);
    if (json) {
      out.print(JsonRenderer.render(result, exitCode));
    } else {
      TextRenderer.render(result, exitCode, quiet, out, err);
    }
    out.flush();
    return exitCode.code();
  }
}
