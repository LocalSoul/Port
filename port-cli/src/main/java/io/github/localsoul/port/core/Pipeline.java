package io.github.localsoul.port.core;

import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.io.IOException;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Lifecycle-Befehle: führt ab einer Phase alle Folgephasen aus ({@code --only}: nur diese). {@code
 * data} enthält {@code phases} (ausgeführt) und je Phase ein Objekt unter ihrem Namen.
 */
public final class Pipeline {

  private final Map<Phase, PhaseStep> steps = new EnumMap<>(Phase.class);

  /** Pipeline mit allen Phasen von M4. */
  public Pipeline() {
    steps.put(Phase.EXTRACT, new ExtractStep());
    steps.put(Phase.IR, new IrStep());
    steps.put(Phase.INDEX, new IndexStep());
    steps.put(Phase.LINT, new LintStep());
    steps.put(Phase.GEN, new GenStep());
    steps.put(Phase.BUILD, new BuildStep());
  }

  /**
   * Führt die Phasen aus.
   *
   * @param command Befehlsname für das Ergebnis
   * @param options Startphase, {@code --only} und weitere Optionen
   * @param context globale Einstellungen
   * @return Ergebnis; bricht eine Phase ab, enthält es die Daten der Phasen davor
   * @throws PortException Exit 2 ohne gültiges Projekt
   */
  public CommandResult run(String command, PipelineOptions options, CommandContext context)
      throws PortException {
    Project project = Project.open(context);
    List<Diagnostic> diagnostics = new ArrayList<>();
    List<String> executed = new ArrayList<>();
    JsonObject.Builder phases = JsonObject.builder();
    for (Phase phase : Phase.values()) {
      if (phase.ordinal() < options.from().ordinal() || !steps.containsKey(phase)) {
        continue;
      }
      if (options.only() && phase != options.from()) {
        break;
      }
      context.progress("port: " + phase.spelling());
      JsonObject data;
      try {
        data = steps.get(phase).run(project, options, context, diagnostics);
        ProjectState.complete(project.dir(), phase);
      } catch (PortException e) {
        diagnostics.add(e.diagnostic());
        executed.add(phase.spelling());
        return CommandResult.failed(command, result(executed, phases), diagnostics, e.exitCode());
      } catch (IOException e) {
        diagnostics.add(Diagnostic.error(DiagnosticIds.IO, e.getMessage(), Project.CACHE, null));
        return CommandResult.failed(
            command, result(executed, phases), diagnostics, ExitCode.GENERAL);
      }
      executed.add(phase.spelling());
      phases.put(phase.spelling(), data);
    }
    JsonObject data = result(executed, phases);
    ExitCode lintFailure = lintFailure(data, options, context);
    if (lintFailure != null) {
      return CommandResult.failed(command, data, diagnostics, lintFailure);
    }
    return CommandResult.ok(command, data, diagnostics);
  }

  /**
   * Lint blockiert die Kette nicht (Nutzerentscheidung M6): Exit 9 nur, wenn {@code lint} die
   * Startphase war und Fehler fand, oder mit {@code --strict} bei Fehlern oder Warnungen.
   */
  private static ExitCode lintFailure(
      JsonObject data, PipelineOptions options, CommandContext context) {
    if (!(data.get(Phase.LINT.spelling()) instanceof JsonObject lint)) {
      return null;
    }
    long errors = ((JsonValue.JsonNumber) lint.get("errors")).value();
    long warnings = ((JsonValue.JsonNumber) lint.get("warnings")).value();
    boolean explicit = options.from() == Phase.LINT;
    if ((explicit && errors > 0) || (context.strict() && errors + warnings > 0)) {
      return ExitCode.LINT;
    }
    return null;
  }

  private static JsonObject result(List<String> executed, JsonObject.Builder phases) {
    JsonObject.Builder data = JsonObject.builder().put("phases", JsonValue.strings(executed));
    for (JsonObject.Member member : phases.build().members()) {
      data.put(member.name(), member.value());
    }
    return data.build();
  }
}
