package io.github.localsoul.port.frontend.unity;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.printer.IrPrinter;
import io.github.localsoul.port.ir.reader.IrReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Übersetzt eine Unity-Assembly über das .NET-Programm in kanonische IR.
 *
 * <p>Ablauf: Programm in ein temporäres Verzeichnis schreiben lassen, jede {@code *.ir}-Datei mit
 * dem {@link IrReader} lesen und mit dem {@link IrPrinter} kanonisieren, Verzeichnis löschen.
 *
 * <p>Fehlerabbildung ({@code docs/cli-contract.md}): Exit 3 des Programms → {@link
 * FrontendException#INPUT_NOT_FOUND}; jeder andere Exit ungleich 0 und ungültige IR → {@link
 * FrontendException#TRANSLATOR_FAILURE}; Programm nicht startbar → {@link
 * FrontendException#GENERAL}. Exit 0 mit raw-Stellen ist Erfolg.
 */
public final class UnityFrontend {

  private final FrontendConfig config;
  private final ToolRunner runner;
  private final IrReader reader = new IrReader();
  private final IrPrinter printer = new IrPrinter();

  /**
   * Erzeugt das Frontend.
   *
   * @param config Programmkonfiguration
   * @param runner Prozessstarter
   */
  public UnityFrontend(FrontendConfig config, ToolRunner runner) {
    this.config = config;
    this.runner = runner;
  }

  /**
   * Frontend mit {@link FrontendConfig#fromEnvironment()} und echtem Prozessstart.
   *
   * @return Frontend
   */
  public static UnityFrontend fromEnvironment() {
    return new UnityFrontend(FrontendConfig.fromEnvironment(), new ProcessToolRunner());
  }

  /**
   * Übersetzt eine Assembly.
   *
   * @param request Auftrag
   * @return kanonische IR je Typ
   * @throws FrontendException bei Programm- oder Formatfehlern, mit Exit-Code laut CLI-Vertrag
   */
  public TranslationResult translate(TranslationRequest request) throws FrontendException {
    Path outDir = createTempDir();
    try {
      List<String> command = command(request, outDir);
      ToolResult result = runner.run(command, config.timeout());
      checkExitCode(result);
      return readOutput(outDir, result.stderr());
    } finally {
      deleteRecursively(outDir);
    }
  }

  /**
   * Baut die Kommandozeile.
   *
   * @param request Auftrag
   * @param outDir Ausgabeverzeichnis
   * @return Programm und Argumente
   */
  List<String> command(TranslationRequest request, Path outDir) {
    List<String> command = new ArrayList<>(config.commandPrefix());
    command.add("--assembly");
    command.add(request.assembly().toString());
    command.add("--out");
    command.add(outDir.toString());
    for (String pattern : request.classes()) {
      command.add("--classes");
      command.add(pattern);
    }
    for (Path reference : request.references()) {
      command.add("--references");
      command.add(reference.toString());
    }
    if (!request.includeSha256()) {
      command.add("--no-sha256");
    }
    return command;
  }

  private static void checkExitCode(ToolResult result) throws FrontendException {
    if (result.exitCode() == 0) {
      return;
    }
    int exitCode =
        result.exitCode() == FrontendException.INPUT_NOT_FOUND
            ? FrontendException.INPUT_NOT_FOUND
            : FrontendException.TRANSLATOR_FAILURE;
    throw new FrontendException(
        exitCode,
        "port-frontend-unity endete mit Exit-Code " + result.exitCode() + ":\n" + result.stderr());
  }

  /**
   * Liest und kanonisiert alle {@code *.ir}-Dateien eines Verzeichnisses.
   *
   * @param dir Verzeichnis mit Rohausgabe
   * @param summary Ausgabe des Programms auf stderr
   * @return Ergebnis
   * @throws FrontendException bei ungültiger IR (mit {@code datei:zeile:spalte}) oder Lesefehlern
   */
  TranslationResult readOutput(Path dir, String summary) throws FrontendException {
    SortedMap<String, IrFile> files = new TreeMap<>();
    SortedMap<String, String> texts = new TreeMap<>();
    for (Path path : irFiles(dir)) {
      String fileName = path.getFileName().toString();
      String typeName = fileName.substring(0, fileName.length() - ".ir".length());
      IrFile file = parse(path, fileName);
      files.put(typeName, file);
      texts.put(typeName, printer.print(file));
    }
    return new TranslationResult(files, texts, summary);
  }

  private IrFile parse(Path path, String fileName) throws FrontendException {
    try {
      String text = Files.readString(path, StandardCharsets.UTF_8);
      return reader.read(text, fileName);
    } catch (IrSyntaxException e) {
      throw new FrontendException(
          FrontendException.TRANSLATOR_FAILURE, "ungültige IR vom Frontend: " + e.getMessage());
    } catch (IOException e) {
      throw new FrontendException(
          FrontendException.GENERAL, "Ausgabe nicht lesbar: " + path + ": " + e.getMessage());
    }
  }

  private static List<Path> irFiles(Path dir) throws FrontendException {
    List<Path> result = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.ir")) {
      for (Path path : stream) {
        result.add(path);
      }
    } catch (IOException e) {
      throw new FrontendException(
          FrontendException.GENERAL, "Ausgabeverzeichnis nicht lesbar: " + e.getMessage());
    }
    result.sort(null);
    return result;
  }

  private static Path createTempDir() throws FrontendException {
    try {
      return Files.createTempDirectory("port-frontend-unity");
    } catch (IOException e) {
      throw new FrontendException(
          FrontendException.GENERAL, "Temporäres Verzeichnis nicht anlegbar: " + e.getMessage());
    }
  }

  /** Löscht die Rohausgabe; Fehler beim Aufräumen ändern das Ergebnis nicht. */
  private static void deleteRecursively(Path dir) {
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        Files.deleteIfExists(path);
      }
      Files.deleteIfExists(dir);
    } catch (IOException e) {
      // Bewusst ignoriert: nur Aufräumen temporärer Dateien.
    }
  }
}
