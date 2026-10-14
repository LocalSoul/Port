package io.github.localsoul.port.core;

import io.github.localsoul.port.generator.java.GeneratedFile;
import io.github.localsoul.port.generator.java.GenerationResult;
import io.github.localsoul.port.generator.java.GeneratorConfig;
import io.github.localsoul.port.generator.java.JavaGenerator;
import io.github.localsoul.port.generator.java.Mapping;
import io.github.localsoul.port.generator.java.MappingException;
import io.github.localsoul.port.generator.java.Todo;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;

/**
 * {@code gen}: IR aus {@code ir/} → Java in {@code gen/} (Generator aus M3, Mappings von Adapter
 * und runtime-system). Paket {@code game.<name>}. Nur geänderte Dateien werden geschrieben, ohne
 * {@code --class} werden veraltete Dateien entfernt.
 */
final class GenStep implements PhaseStep {

  static final String DIR = "gen";

  @Override
  public JsonObject run(
      Project project,
      PipelineOptions options,
      CommandContext context,
      List<Diagnostic> diagnostics)
      throws PortException {
    List<IrFile> irFiles = IrFiles.read(project.dir());
    Path adapter =
        project.platform().adapter(options.adapter(), project.manifest().requiresAdapter());
    Mapping mapping = mapping(project.platform().mappings(adapter));
    Set<String> manualTypes = manualTypes(project.dir(), irFiles);
    GeneratorConfig config =
        new GeneratorConfig(basePackage(project.manifest().name()), mapping, manualTypes);
    GenerationResult result = new JavaGenerator(config).generate(irFiles);
    Set<String> selected = selectedSimpleNames(irFiles, options.classFilter());
    Path genDir = project.resolve(DIR);
    List<String> files = new ArrayList<>();
    List<String> written = new ArrayList<>();
    List<String> removed = new ArrayList<>();
    try {
      for (GeneratedFile file : result.files()) {
        if (!selected.contains(typeOfFile(file.path()))) {
          continue;
        }
        String relative = DIR + "/" + file.path();
        files.add(relative);
        if (ProjectFiles.writeAtomic(project.resolve(relative), file.content())) {
          written.add(relative);
        }
      }
      if (options.classFilter() == null) {
        removeStale(project.dir(), genDir, new TreeSet<>(files), removed);
      }
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL, Diagnostic.error(DiagnosticIds.IO, e.getMessage(), DIR, null));
    }
    int todoCount = 0;
    for (Todo todo : result.todos()) {
      if (selected.contains(ManualWork.simpleName(ManualWork.typeOf(todo)))) {
        diagnostics.add(diagnostic(todo, manualTypes));
        todoCount++;
      }
    }
    return JsonObject.builder()
        .put("files", JsonValue.strings(files))
        .put("todoCount", todoCount)
        .put("written", JsonValue.strings(written))
        .put("removed", JsonValue.strings(removed))
        .build();
  }

  /** Paket der erzeugten Klassen: {@code game.<name>} ({@code -} wird {@code _}). */
  static String basePackage(String projectName) {
    String name = projectName.replace('-', '_');
    if (Character.isDigit(name.charAt(0))) {
      name = "_" + name;
    }
    return "game." + name;
  }

  private static Mapping mapping(List<Path> files) throws PortException {
    try {
      Mapping merged = Mapping.empty();
      for (Path file : files) {
        merged = merged.merge(Mapping.load(file));
      }
      return merged;
    } catch (IOException | MappingException e) {
      throw new PortException(
          ExitCode.DEPENDENCY,
          Diagnostic.error(
              DiagnosticIds.PLATFORM, "Mapping ungültig: " + e.getMessage(), null, null));
    }
  }

  /** IR-Typen, für die {@code manual/<Typ>.java} existiert. */
  static Set<String> manualTypes(Path projectDir, List<IrFile> irFiles) throws PortException {
    Set<String> classes;
    try {
      classes = ManualWork.classNames(projectDir);
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL,
          Diagnostic.error(DiagnosticIds.IO, e.getMessage(), ManualWork.DIR, null));
    }
    Set<String> types = new TreeSet<>();
    for (IrFile file : irFiles) {
      for (TypeDecl type : file.types()) {
        if (classes.contains(ManualWork.simpleName(type.name()))) {
          types.add(type.name());
        }
      }
    }
    return types;
  }

  private static Set<String> selectedSimpleNames(List<IrFile> irFiles, String classFilter) {
    Pattern pattern = classFilter == null ? null : glob(classFilter);
    Set<String> names = new TreeSet<>();
    for (IrFile file : irFiles) {
      for (TypeDecl type : file.types()) {
        if (pattern == null || pattern.matcher(type.name()).matches()) {
          names.add(ManualWork.simpleName(type.name()));
        }
      }
    }
    return names;
  }

  static Pattern glob(String text) {
    StringBuilder regex = new StringBuilder();
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      if (c == '*') {
        regex.append(".*");
      } else if (c == '?') {
        regex.append('.');
      } else {
        regex.append(Pattern.quote(String.valueOf(c)));
      }
    }
    return Pattern.compile(regex.toString());
  }

  /** {@code a/b/FooBase.java} → {@code Foo}. */
  private static String typeOfFile(String path) {
    String name = path.substring(path.lastIndexOf('/') + 1, path.length() - ".java".length());
    return name.endsWith("Base") ? name.substring(0, name.length() - "Base".length()) : name;
  }

  private static Diagnostic diagnostic(Todo todo, Set<String> manualTypes) {
    String type = ManualWork.typeOf(todo);
    String location = IrStep.DIR + "/" + type + ".ir";
    boolean manual = ManualWork.isManual(todo) || ManualWork.isClassNotice(todo);
    if (!manual) {
      return new Diagnostic(
          DiagnosticIds.TODO, Severity.WARN, todo.symbol(), todo.reason(), location, null);
    }
    if (manualTypes.contains(type)) {
      return new Diagnostic(
          DiagnosticIds.TODO,
          Severity.INFO,
          todo.symbol(),
          "durch manual/ abgedeckt: " + todo.reason(),
          location,
          null);
    }
    String simple = ManualWork.simpleName(type);
    return new Diagnostic(
        DiagnosticIds.MANUAL,
        Severity.WARN,
        todo.symbol(),
        todo.reason(),
        location,
        "manual/.../"
            + simple
            + ".java mit class "
            + simple
            + " extends "
            + simple
            + "Base anlegen");
  }

  private static void removeStale(
      Path projectDir, Path genDir, Set<String> keep, List<String> removed) throws IOException {
    for (Path file : ManualWork.javaFiles(genDir)) {
      String relative = ProjectFiles.relative(projectDir, file);
      if (!keep.contains(relative)) {
        Files.delete(file);
        removed.add(relative);
      }
    }
  }
}
