package io.github.localsoul.port.core;

import io.github.localsoul.launcher.manifest.ProjectManifest;
import io.github.localsoul.port.json.JsonObject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/**
 * {@code extract}: prüft die Eingabe (vorhanden, SHA-256 laut Manifest) und hält sie in {@code
 * .port/extract.tsv} fest. Assets folgen in M8.
 */
final class ExtractStep implements PhaseStep {

  static final String FILE = Project.CACHE + "/extract.tsv";

  @Override
  public JsonObject run(
      Project project,
      PipelineOptions options,
      CommandContext context,
      List<Diagnostic> diagnostics)
      throws PortException {
    String source = project.manifest().source();
    Path input = project.resolve(source);
    if (!Files.isRegularFile(input)) {
      throw new PortException(
          ExitCode.INPUT,
          Diagnostic.error(
              DiagnosticIds.INPUT,
              "Eingabe nicht gefunden: " + source,
              source,
              "Datei nach " + source + " kopieren (wird nicht committet)"));
    }
    String sha256;
    long size;
    try {
      sha256 = ProjectFiles.sha256(input);
      size = Files.size(input);
    } catch (IOException e) {
      throw new PortException(
          ExitCode.INPUT,
          Diagnostic.error(
              DiagnosticIds.INPUT, "Eingabe nicht lesbar: " + e.getMessage(), source, null));
    }
    String expected = project.manifest().sha256();
    if (expected == null) {
      diagnostics.add(
          new Diagnostic(
              DiagnosticIds.HASH,
              Severity.INFO,
              null,
              "kein sha256 in port.toml, Eingabe wird nicht geprüft",
              ProjectManifest.FILE,
              "sha256 = \"" + sha256 + "\" unter [game] eintragen"));
    } else if (!expected.equals(sha256)) {
      throw new PortException(
          ExitCode.INPUT,
          Diagnostic.error(
              DiagnosticIds.HASH,
              "SHA-256 von " + source + " stimmt nicht mit port.toml überein (" + sha256 + ")",
              source,
              "richtige Eingabe kopieren oder sha256 in port.toml bewusst anpassen"));
    }
    try {
      ProjectFiles.writeAtomic(
          project.resolve(FILE),
          "file\tsize\tsha256\n" + source + "\t" + size + "\t" + sha256 + "\n");
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL, Diagnostic.error(DiagnosticIds.IO, e.getMessage(), FILE, null));
    }
    return JsonObject.builder()
        .put("source", source)
        .put("size", size)
        .put("sha256", sha256)
        .build();
  }
}
