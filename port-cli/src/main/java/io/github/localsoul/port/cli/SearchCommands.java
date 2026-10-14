package io.github.localsoul.port.cli;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.DiagnosticIds;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.core.PortException;
import io.github.localsoul.port.core.SearchService;
import java.util.List;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.OptionSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

/** Suche und Navigation auf dem Index: {@code find}, {@code refs}, {@code callers}, ... */
final class SearchCommands {

  private SearchCommands() {}

  /** Art eines Listenbefehls. */
  private enum Kind {
    FIND("find", "<text>", "Symbole, deren Name oder ID den Text enthält (ohne Groß/Klein)."),
    REFS("refs", "<symbol>", "Alle Verwendungen eines Typs oder Members."),
    CALLERS("callers", "<methode>", "Symbole, die die Methode aufrufen."),
    CALLEES("callees", "<methode>", "Von der Methode aufgerufene Methoden."),
    USES("uses", "<api>", "Stellen, die eine externe API nutzen (Symbol oder Typ).");

    final String name;
    final String label;
    final String description;

    Kind(String name, String label, String description) {
      this.name = name;
      this.label = label;
      this.description = description;
    }
  }

  static List<PortCommand> all() {
    return List.of(
        new ListCommand(Kind.FIND),
        new ListCommand(Kind.REFS),
        new ListCommand(Kind.CALLERS),
        new ListCommand(Kind.CALLEES),
        new ListCommand(Kind.USES),
        new ShowCommand(),
        new OutlineCommand());
  }

  private static PositionalParamSpec argument(String label) {
    return PositionalParamSpec.builder().paramLabel(label).arity("1").type(String.class).build();
  }

  /** Listenbefehl mit {@code --limit} und {@code --cursor}. */
  private static final class ListCommand implements PortCommand {

    private final Kind kind;

    ListCommand(Kind kind) {
      this.kind = kind;
    }

    @Override
    public String name() {
      return kind.name;
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name(kind.name);
      spec.usageMessage().description(kind.description);
      spec.addPositional(argument(kind.label));
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
      String value = args.matchedPositionalValue(0, "");
      Integer limit = args.matchedOptionValue("--limit", 0);
      String cursor = args.matchedOptionValue("--cursor", null);
      return switch (kind) {
        case FIND -> SearchService.find(context, value, limit, cursor);
        case REFS -> SearchService.refs(context, value, limit, cursor);
        case CALLERS -> SearchService.callers(context, value, limit, cursor);
        case CALLEES -> SearchService.callees(context, value, limit, cursor);
        case USES -> SearchService.uses(context, value, limit, cursor);
      };
    }
  }

  /** {@code port show <symbol> [--ir|--java|--src]}. */
  private static final class ShowCommand implements PortCommand {

    @Override
    public String name() {
      return "show";
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name("show");
      spec.usageMessage()
          .description("Deklaration eines Symbols mit IR-Text (--java: erzeugte Datei).");
      spec.addPositional(argument("<symbol>"));
      for (String view : new String[] {"--ir", "--java", "--src"}) {
        spec.addOption(OptionSpec.builder(view).arity("0").type(boolean.class).build());
      }
      return spec;
    }

    @Override
    public CommandResult run(ParseResult args, CommandContext context) throws PortException {
      int views = 0;
      String view = "ir";
      for (String option : new String[] {"--ir", "--java", "--src"}) {
        if (args.hasMatchedOption(option)) {
          views++;
          view = option.substring(2);
        }
      }
      if (views > 1) {
        throw new PortException(
            ExitCode.INVALID,
            Diagnostic.error(DiagnosticIds.ARGS, "nur eine von --ir, --java, --src", null, null));
      }
      return SearchService.show(context, args.matchedPositionalValue(0, ""), view);
    }
  }

  /** {@code port outline <klasse>}. */
  private static final class OutlineCommand implements PortCommand {

    @Override
    public String name() {
      return "outline";
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name("outline");
      spec.usageMessage().description("Mitglieder eines Typs in Deklarationsreihenfolge.");
      spec.addPositional(argument("<klasse>"));
      return spec;
    }

    @Override
    public CommandResult run(ParseResult args, CommandContext context) throws PortException {
      return SearchService.outline(context, args.matchedPositionalValue(0, ""));
    }
  }
}
