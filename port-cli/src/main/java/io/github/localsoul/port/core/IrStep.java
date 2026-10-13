package io.github.localsoul.port.core;

import io.github.localsoul.port.frontend.unity.FrontendException;
import io.github.localsoul.port.frontend.unity.ProcessToolRunner;
import io.github.localsoul.port.frontend.unity.TranslationRequest;
import io.github.localsoul.port.frontend.unity.TranslationResult;
import io.github.localsoul.port.frontend.unity.UnityFrontend;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.validate.IrValidator;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * {@code ir}: ruft das Unity-Frontend (M2) auf und schreibt je Typ {@code ir/<Typ>.ir} kanonisch.
 * Unveränderte Dateien bleiben unberührt, Dateien nicht mehr vorhandener Typen werden entfernt.
 */
final class IrStep implements PhaseStep {

  static final String DIR = "ir";

  @Override
  public JsonObject run(
      Project project,
      PipelineOptions options,
      CommandContext context,
      List<Diagnostic> diagnostics)
      throws PortException {
    Path assembly = project.resolve(project.manifest().source());
    TranslationResult translated = translate(project, assembly);
    Path irDir = project.resolve(DIR);
    List<String> written = new ArrayList<>();
    Set<String> expected = new TreeSet<>();
    try {
      for (Map.Entry<String, String> entry : translated.canonicalTexts().entrySet()) {
        String file = entry.getKey() + ".ir";
        expected.add(file);
        if (ProjectFiles.writeAtomic(irDir.resolve(file), entry.getValue())) {
          written.add(DIR + "/" + file);
        }
      }
      List<String> removed = removeStale(irDir, expected);
      reportUnresolvedTypes(translated, diagnostics);
      return JsonObject.builder()
          .put("types", translated.canonicalTexts().size())
          .put("written", JsonValue.strings(written))
          .put("removed", JsonValue.strings(removed))
          .build();
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL, Diagnostic.error(DiagnosticIds.IO, e.getMessage(), DIR, null));
    }
  }

  /**
   * Eine Warnung je Typ mit {@code unknown} (Validator-Regel IR003): meist fehlen referenzierte
   * Assemblies in {@code input/}.
   */
  private static void reportUnresolvedTypes(
      TranslationResult translated, List<Diagnostic> diagnostics) {
    IrValidator validator = new IrValidator();
    for (Map.Entry<String, IrFile> entry : translated.files().entrySet()) {
      int unresolved = 0;
      for (io.github.localsoul.port.ir.validate.Diagnostic found :
          validator.validate(entry.getValue())) {
        if (found.id().equals("IR003")) {
          unresolved++;
        }
      }
      if (unresolved > 0) {
        diagnostics.add(
            new Diagnostic(
                DiagnosticIds.UNRESOLVED,
                Severity.WARN,
                entry.getKey(),
                unresolved + " Stelle(n) mit nicht aufgelöstem Typ (IR003, unknown)",
                DIR + "/" + entry.getKey() + ".ir",
                "referenzierte Assemblies (bei Unity Managed/*.dll) nach input/ legen"));
      }
    }
  }

  private static TranslationResult translate(Project project, Path assembly) throws PortException {
    UnityFrontend frontend =
        new UnityFrontend(project.platform().frontend(), new ProcessToolRunner());
    try {
      return frontend.translate(TranslationRequest.of(assembly));
    } catch (FrontendException e) {
      ExitCode code = ExitCode.of(e.exitCode());
      String hint =
          code == ExitCode.GENERAL
              ? "scripts/build-dotnet.sh ausführen oder PORT_FRONTEND_UNITY setzen"
              : null;
      throw new PortException(
          code,
          Diagnostic.error(
              DiagnosticIds.FRONTEND, e.getMessage(), project.manifest().source(), hint));
    }
  }

  private static List<String> removeStale(Path irDir, Set<String> expected) throws IOException {
    List<String> removed = new ArrayList<>();
    if (!Files.isDirectory(irDir)) {
      return removed;
    }
    List<Path> existing = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(irDir, "*.ir")) {
      for (Path path : stream) {
        existing.add(path);
      }
    }
    existing.sort(null);
    for (Path path : existing) {
      String name = path.getFileName().toString();
      if (!expected.contains(name)) {
        Files.delete(path);
        removed.add(DIR + "/" + name);
      }
    }
    return removed;
  }
}
