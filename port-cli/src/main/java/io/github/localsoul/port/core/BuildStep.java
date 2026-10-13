package io.github.localsoul.port.core;

import io.github.localsoul.port.json.JsonObject;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TreeSet;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/**
 * {@code build}: kompiliert {@code gen/} und {@code manual/} mit {@code javax.tools} gegen Adapter
 * und Plattform nach {@code .port/classes}. Kompiliert wird in einen temporären Ordner, danach
 * werden nur geänderte Klassen übernommen (idempotent). Fehler → Exit 7.
 */
final class BuildStep implements PhaseStep {

  static final String OUTPUT = Project.CACHE + "/classes";

  private static final String STAGING = Project.CACHE + "/classes.tmp";

  @Override
  public JsonObject run(
      Project project,
      PipelineOptions options,
      CommandContext context,
      List<Diagnostic> diagnostics)
      throws PortException {
    Path adapter =
        project.platform().adapter(options.adapter(), project.manifest().requiresAdapter());
    List<Path> classpath = project.platform().classpath(adapter);
    List<Path> sources;
    try {
      sources = new ArrayList<>(ManualWork.javaFiles(project.resolve(GenStep.DIR)));
      sources.addAll(ManualWork.javaFiles(project.resolve(ManualWork.DIR)));
    } catch (IOException e) {
      throw io(e);
    }
    if (sources.isEmpty()) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.COMPILE,
              "keine Quellen in gen/ oder manual/",
              GenStep.DIR,
              "port gen"));
    }
    Path staging = project.resolve(STAGING);
    try {
      deleteTree(staging);
      Files.createDirectories(staging);
      compile(project.dir(), sources, classpath, staging, diagnostics);
      int classes = sync(staging, project.resolve(OUTPUT));
      deleteTree(staging);
      return JsonObject.builder()
          .put("sources", sources.size())
          .put("classes", classes)
          .put("output", OUTPUT)
          .build();
    } catch (IOException e) {
      throw io(e);
    }
  }

  private static void compile(
      Path projectDir,
      List<Path> sources,
      List<Path> classpath,
      Path output,
      List<Diagnostic> diagnostics)
      throws PortException, IOException {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    if (compiler == null) {
      throw new PortException(
          ExitCode.GENERAL,
          Diagnostic.error(
              DiagnosticIds.COMPILE, "kein Java-Compiler (JDK statt JRE nötig)", null, null));
    }
    DiagnosticCollector<JavaFileObject> collector = new DiagnosticCollector<>();
    StringBuilder path = new StringBuilder();
    for (Path entry : classpath) {
      path.append(path.isEmpty() ? "" : java.io.File.pathSeparator).append(entry);
    }
    List<String> arguments =
        List.of(
            "-classpath",
            path.toString(),
            "-d",
            output.toString(),
            "-encoding",
            "UTF-8",
            "-proc:none",
            "-Xlint:none",
            "-implicit:none");
    boolean ok;
    try (StandardJavaFileManager files =
        compiler.getStandardFileManager(collector, Locale.ROOT, null)) {
      Iterable<? extends JavaFileObject> units = files.getJavaFileObjectsFromPaths(sources);
      ok = compiler.getTask(new StringWriter(), files, collector, arguments, null, units).call();
    }
    int errors = 0;
    for (javax.tools.Diagnostic<? extends JavaFileObject> found : collector.getDiagnostics()) {
      if (found.getKind() == javax.tools.Diagnostic.Kind.ERROR) {
        diagnostics.add(compileError(projectDir, found));
        errors++;
      }
    }
    if (!ok) {
      throw new PortException(
          ExitCode.COMPILE,
          Diagnostic.error(
              DiagnosticIds.COMPILE,
              errors + " Kompilierfehler im generierten oder handgeschriebenen Java",
              null,
              "Fehler in manual/ beheben oder Generator-Todos prüfen"));
    }
  }

  private static Diagnostic compileError(
      Path projectDir, javax.tools.Diagnostic<? extends JavaFileObject> found) {
    String location = null;
    if (found.getSource() != null) {
      Path file = Path.of(found.getSource().toUri());
      location =
          ProjectFiles.relative(projectDir, file)
              + ":"
              + found.getLineNumber()
              + ":"
              + found.getColumnNumber();
    }
    return Diagnostic.error(DiagnosticIds.COMPILE, found.getMessage(Locale.ROOT), location, null);
  }

  /** Übernimmt Klassen aus {@code staging} nach {@code target}, entfernt veraltete. */
  private static int sync(Path staging, Path target) throws IOException {
    Set<String> fresh = new TreeSet<>();
    for (Path file : files(staging)) {
      String relative = ProjectFiles.relative(staging, file);
      fresh.add(relative);
      ProjectFiles.writeAtomic(target.resolve(relative), Files.readAllBytes(file));
    }
    for (Path file : files(target)) {
      if (!fresh.contains(ProjectFiles.relative(target, file))) {
        Files.delete(file);
      }
    }
    return fresh.size();
  }

  private static List<Path> files(Path dir) throws IOException {
    List<Path> result = new ArrayList<>();
    collect(dir, result);
    result.sort(null);
    return result;
  }

  private static void collect(Path dir, List<Path> result) throws IOException {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        if (Files.isDirectory(path)) {
          collect(path, result);
        } else {
          result.add(path);
        }
      }
    }
  }

  private static void deleteTree(Path dir) throws IOException {
    if (!Files.exists(dir)) {
      return;
    }
    if (Files.isDirectory(dir)) {
      try (java.nio.file.DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
        for (Path path : stream) {
          deleteTree(path);
        }
      }
    }
    Files.delete(dir);
  }

  private static PortException io(IOException e) {
    return new PortException(
        ExitCode.GENERAL, Diagnostic.error(DiagnosticIds.IO, e.getMessage(), Project.CACHE, null));
  }
}
