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

  /** Systemeigenschaft mit der Plattformwurzel (Vorrang vor {@link #HOME_ENVIRONMENT}). */
  static final String HOME_PROPERTY = "port.home";

  /** Umgebungsvariable mit der Plattformwurzel, wenn das Projekt außerhalb des Monorepos liegt. */
  static final String HOME_ENVIRONMENT = "PORT_HOME";

  private final List<PortCommand> commands;
  private final Path platformHome;

  Main(List<PortCommand> commands, Path platformHome) {
    this.commands = List.copyOf(commands);
    this.platformHome = platformHome;
  }

  /**
   * Startet die CLI und beendet die JVM mit dem Exit-Code.
   *
   * @param args Kommandozeile
   */
  public static void main(String[] args) {
    Path workingDir = Path.of("").toAbsolutePath();
    int exitCode =
        new Main(Commands.all(), configuredHome(workingDir))
            .run(args, System.out, System.err, workingDir);
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
    List<ParseResult> chain = chain(parsed);
    ParseResult sub = chain.get(chain.size() - 1);
    if (chain.size() == 1) {
      return failArgs("port", "Befehl fehlt", flag(chain, JSON), out, err);
    }
    if (sub.isUsageHelpRequested()) {
      sub.commandSpec().commandLine().usage(out);
      return ExitCode.SUCCESS.code();
    }
    String commandName = commandName(chain);
    if (!hasCommand(commandName)) {
      return failArgs(commandName, "Unterbefehl fehlt", flag(chain, JSON), out, err);
    }
    boolean json = flag(chain, JSON);
    CommandContext context = context(chain, workingDir, err, platformHome);
    PortCommand command = command(commandName);
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

  private static Path configuredHome(Path workingDir) {
    String home = System.getProperty(HOME_PROPERTY);
    if (home == null || home.isBlank()) {
      home = System.getenv(HOME_ENVIRONMENT);
    }
    if (home == null || home.isBlank()) {
      return null;
    }
    return workingDir.resolve(home).toAbsolutePath().normalize();
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
      String name = command.name();
      int space = name.indexOf(' ');
      if (space < 0) {
        root.addSubcommand(name, command.spec());
        continue;
      }
      String group = name.substring(0, space);
      CommandSpec groupSpec =
          root.subcommands().containsKey(group)
              ? root.subcommands().get(group).getCommandSpec()
              : addGroup(root, group);
      groupSpec.addSubcommand(name.substring(space + 1), command.spec());
    }
    return root;
  }

  /** Gruppe wie {@code catalog} ohne eigene Ausführung, nur mit Unterbefehlen. */
  private static CommandSpec addGroup(CommandSpec root, String group) {
    CommandSpec spec = CommandSpec.create().name(group);
    spec.usageMessage().description("Befehle unter " + group + " (port " + group + " --help)");
    root.addSubcommand(group, spec);
    return root.subcommands().get(group).getCommandSpec();
  }

  private static OptionSpec flag(String name, String description) {
    return OptionSpec.builder(name)
        .arity("0")
        .type(boolean.class)
        .scopeType(ScopeType.INHERIT)
        .description(description)
        .build();
  }

  /** Wurzel, ggf. Gruppe und Befehl in Aufrufreihenfolge. */
  private static List<ParseResult> chain(ParseResult root) {
    List<ParseResult> chain = new ArrayList<>();
    ParseResult current = root;
    while (current != null) {
      chain.add(current);
      current = current.subcommand();
    }
    return chain;
  }

  /** Befehlsname ohne {@code port}, z. B. {@code catalog list}. */
  private static String commandName(List<ParseResult> chain) {
    StringBuilder name = new StringBuilder();
    for (int i = 1; i < chain.size(); i++) {
      name.append(i == 1 ? "" : " ").append(chain.get(i).commandSpec().name());
    }
    return name.toString();
  }

  /** Globale Flags dürfen vor, zwischen oder nach den Befehlswörtern stehen. */
  private static boolean flag(List<ParseResult> chain, String name) {
    for (ParseResult result : chain) {
      if (result.hasMatchedOption(name)) {
        return true;
      }
    }
    return false;
  }

  private static CommandContext context(
      List<ParseResult> chain, Path workingDir, PrintStream err, Path platformHome) {
    String directory = null;
    for (ParseResult result : chain) {
      String value = result.matchedOptionValue(DIRECTORY, null);
      if (value != null) {
        directory = value;
      }
    }
    Path projectDir = directory == null ? workingDir : workingDir.resolve(directory);
    return new CommandContext(
        workingDir,
        projectDir.toAbsolutePath().normalize(),
        flag(chain, STRICT),
        flag(chain, VERBOSE),
        flag(chain, QUIET),
        flag(chain, OFFLINE),
        err,
        platformHome);
  }

  private boolean hasCommand(String name) {
    for (PortCommand command : commands) {
      if (command.name().equals(name)) {
        return true;
      }
    }
    return false;
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
    if (spec.parent() == null) {
      return "port";
    }
    if (spec.parent().parent() == null) {
      return spec.name();
    }
    return spec.parent().name() + " " + spec.name();
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
