package io.github.localsoul.port.core;

import io.github.localsoul.launcher.manifest.LintSettings;
import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.reader.IrReader;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.lint.Coverage;
import io.github.localsoul.port.lint.IrSource;
import io.github.localsoul.port.lint.LintConfig;
import io.github.localsoul.port.lint.LintFinding;
import io.github.localsoul.port.lint.LintInput;
import io.github.localsoul.port.lint.Linter;
import io.github.localsoul.port.lint.ManualOverride;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * {@code lint}: prüft {@code ir/} mit den Regeln aus {@code lint} ({@code docs/modules/lint.md}).
 * Abdeckung: jeder Eintrag in {@code mapping.tsv} von Adapter und {@code runtime-system} gilt als
 * {@code full}, {@code adapter-metadata.json} kennzeichnet Stubs. Befunde werden Diagnosen; über
 * den Exit-Code entscheidet die {@link Pipeline}.
 */
final class LintStep implements PhaseStep {

  static final String METADATA = "src/main/resources/adapter-metadata.json";

  private static final Pattern METADATA_ENTRY =
      Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"status\"\\s*:\\s*\"(full|stub)\"");

  @Override
  public JsonObject run(
      Project project,
      PipelineOptions options,
      CommandContext context,
      List<Diagnostic> diagnostics)
      throws PortException {
    Path adapter =
        project.platform().adapter(options.adapter(), project.manifest().requiresAdapter());
    Coverage coverage = coverage(adapter, project.platform().mappings(adapter));
    LintInput input = new LintInput(sources(project), overrides(project), coverage);
    List<LintFinding> findings = Linter.lint(input, config(project.manifest().lint()));
    PipelineOptions.LintOptions lint = options.lint();
    Pattern classFilter = lint.classFilter() == null ? null : GenStep.glob(lint.classFilter());
    long errors = 0;
    long warnings = 0;
    long infos = 0;
    for (LintFinding finding : findings) {
      if (!lint.rules().isEmpty() && !lint.rules().contains(finding.id())) {
        continue;
      }
      if (classFilter != null && !matches(classFilter, finding.symbol())) {
        continue;
      }
      switch (finding.severity()) {
        case ERROR -> errors++;
        case WARN -> warnings++;
        case INFO -> infos++;
      }
      diagnostics.add(
          new Diagnostic(
              finding.id(),
              Severity.valueOf(finding.severity().name()),
              finding.symbol(),
              finding.message(),
              finding.location(),
              lint.fixHints() ? finding.hint() : null));
    }
    return JsonObject.builder()
        .put("errors", errors)
        .put("warnings", warnings)
        .put("infos", infos)
        .build();
  }

  private static boolean matches(Pattern pattern, String symbol) {
    if (symbol == null) {
      return false;
    }
    int separator = symbol.indexOf("::");
    return pattern.matcher(separator < 0 ? symbol : symbol.substring(0, separator)).matches();
  }

  private static LintConfig config(LintSettings settings) {
    Map<String, io.github.localsoul.port.lint.Severity> severity = new TreeMap<>();
    for (Map.Entry<String, String> entry : settings.severity().entrySet()) {
      severity.put(
          entry.getKey(), io.github.localsoul.port.lint.Severity.fromSpelling(entry.getValue()));
    }
    Set<String> disable = new TreeSet<>(settings.disable());
    return new LintConfig(disable, severity);
  }

  /** IR-Dateien, Syntaxfehler werden zu IR001 statt zum Abbruch. */
  private static List<IrSource> sources(Project project) throws PortException {
    List<IrSource> sources = new ArrayList<>();
    Path dir = project.resolve(IrStep.DIR);
    List<Path> paths = new ArrayList<>();
    try {
      if (Files.isDirectory(dir)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.ir")) {
          for (Path path : stream) {
            paths.add(path);
          }
        }
      }
      paths.sort(null);
      IrReader reader = new IrReader();
      for (Path path : paths) {
        String name = IrStep.DIR + "/" + path.getFileName();
        String text = Files.readString(path, StandardCharsets.UTF_8);
        try {
          sources.add(new IrSource(name, text, reader.read(text, name), null));
        } catch (IrSyntaxException e) {
          sources.add(new IrSource(name, text, null, e));
        }
      }
    } catch (IOException e) {
      throw io(e, IrStep.DIR);
    }
    return sources;
  }

  private static List<ManualOverride> overrides(Project project) throws PortException {
    List<ManualOverride> overrides = new ArrayList<>();
    try {
      for (Path file : ManualWork.javaFiles(project.resolve(ManualWork.DIR))) {
        String path = ProjectFiles.relative(project.dir(), file);
        overrides.addAll(
            ManualOverride.parse(path, Files.readString(file, StandardCharsets.UTF_8)));
      }
    } catch (IOException e) {
      throw io(e, ManualWork.DIR);
    }
    return overrides;
  }

  /** Stubs aus den Metadaten zuerst, danach alle Mapping-Einträge als {@code full}. */
  private static Coverage coverage(Path adapter, List<Path> mappings) throws PortException {
    Coverage coverage = new Coverage();
    Path metadata = adapter.resolve(METADATA);
    try {
      if (Files.isRegularFile(metadata)) {
        Matcher matcher =
            METADATA_ENTRY.matcher(Files.readString(metadata, StandardCharsets.UTF_8));
        while (matcher.find()) {
          coverage.put(matcher.group(1), matcher.group(2));
        }
      }
      for (Path mapping : mappings) {
        for (String line : Files.readAllLines(mapping, StandardCharsets.UTF_8)) {
          int tab = line.indexOf('\t');
          if (!line.isBlank() && !line.startsWith("#") && tab > 0) {
            coverage.put(line.substring(0, tab), Coverage.FULL);
          }
        }
      }
    } catch (IOException e) {
      throw io(e, adapter.toString());
    }
    return coverage;
  }

  private static PortException io(IOException e, String location) {
    return new PortException(
        ExitCode.GENERAL, Diagnostic.error(DiagnosticIds.IO, e.getMessage(), location, null));
  }
}
