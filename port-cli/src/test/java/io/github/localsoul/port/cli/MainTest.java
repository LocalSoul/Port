package io.github.localsoul.port.cli;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.core.CommandContext;
import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.DiagnosticIds;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.core.PortException;
import io.github.localsoul.port.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Model.PositionalParamSpec;
import picocli.CommandLine.ParseResult;

/** Aufruf, globale Flags, Hülle und Exit-Codes mit einem Testbefehl. */
class MainTest {

  private static final Path WORK = Path.of("/work").toAbsolutePath();

  /** Gibt Projektordner und Flags als Daten zurück; Argument „fail“ bricht mit Exit 3 ab. */
  private static final class EchoCommand implements PortCommand {

    @Override
    public String name() {
      return "echo";
    }

    @Override
    public CommandSpec spec() {
      CommandSpec spec = CommandSpec.create().name("echo");
      spec.addPositional(PositionalParamSpec.builder().arity("0..1").type(String.class).build());
      return spec;
    }

    @Override
    public CommandResult run(ParseResult args, CommandContext context) throws PortException {
      String mode = args.matchedPositionalValue(0, "ok");
      if (mode.equals("fail")) {
        throw new PortException(
            ExitCode.INPUT, Diagnostic.error(DiagnosticIds.INPUT, "fehlt", "input/x.dll", null));
      }
      JsonObject data =
          JsonObject.builder()
              .put("projectDir", context.projectDir().toString())
              .put("strict", context.strict())
              .build();
      List<Diagnostic> diagnostics =
          mode.equals("manual")
              ? List.of(Diagnostic.warn(DiagnosticIds.MANUAL, "Handarbeit offen", "ir/A.ir"))
              : List.of();
      return CommandResult.ok("echo", data, diagnostics);
    }
  }

  private record Run(int exitCode, String out, String err) {}

  private static Run run(String... args) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    ByteArrayOutputStream err = new ByteArrayOutputStream();
    Main main = new Main(List.of(new EchoCommand()));
    int exitCode =
        main.run(
            args,
            new PrintStream(out, true, StandardCharsets.UTF_8),
            new PrintStream(err, true, StandardCharsets.UTF_8),
            WORK);
    return new Run(
        exitCode, out.toString(StandardCharsets.UTF_8), err.toString(StandardCharsets.UTF_8));
  }

  @Test
  void jsonPrintsExactlyOneObjectAndNothingElse() throws Exception {
    Run run = run("--json", "-v", "-C", "games/demo", "echo");
    assertEquals(0, run.exitCode());
    assertEquals("", run.err());
    Golden.assertMatches("main-echo.json", run.out().replace(WORK.toString(), "<work>"));
  }

  @Test
  void globalFlagsWorkAfterTheCommand() {
    Run before = run("--json", "--strict", "echo");
    Run after = run("echo", "--json", "--strict");
    assertEquals(before, after);
    assertTrue(after.out().contains("\"strict\": true"), after.out());
  }

  @Test
  void strictTurnsOpenManualWorkIntoExit5() {
    assertEquals(0, run("--json", "echo", "manual").exitCode());
    Run strict = run("--json", "--strict", "echo", "manual");
    assertEquals(5, strict.exitCode());
    assertTrue(strict.out().contains("\"ok\": false"), strict.out());
  }

  @Test
  void abortedCommandReportsItsExitCodeAndDiagnostic() {
    Run run = run("--json", "echo", "fail");
    assertEquals(3, run.exitCode());
    assertTrue(run.out().contains("\"id\": \"PORT-INPUT\""), run.out());
  }

  @Test
  void invalidArgumentsGiveExit2InBothFormats() throws Exception {
    Run unknown = run("--json", "gibtsnicht");
    assertEquals(2, unknown.exitCode());
    Golden.assertMatches("main-unknown-command.json", unknown.out());
    Run missing = run("--json");
    assertEquals(2, missing.exitCode());
    Run text = run("echo", "a", "b");
    assertEquals(2, text.exitCode());
    assertTrue(text.err().startsWith("error PORT-ARGS"), text.err());
  }

  @Test
  void helpGoesToStdoutWithExit0() {
    Run run = run("--help");
    assertEquals(0, run.exitCode());
    assertTrue(run.out().contains("echo"), run.out());
    assertEquals(0, run("echo", "--help").exitCode());
  }

  @Test
  void textOutputSeparatesDataAndDiagnostics() {
    Run run = run("echo", "manual");
    assertTrue(run.out().contains("strict: false"), run.out());
    assertTrue(run.out().contains("port echo: ok (Exit 0)"), run.out());
    assertTrue(run.err().contains("warn PORT-MANUAL ir/A.ir: Handarbeit offen"), run.err());
    Run quiet = run("-q", "echo", "manual");
    assertEquals("", quiet.out());
    assertEquals("", quiet.err());
  }
}
