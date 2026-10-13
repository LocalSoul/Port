package io.github.localsoul.port.frontend.unity;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * {@link ToolRunner} über {@link ProcessBuilder}. stderr wird in eine temporäre Datei umgeleitet
 * (kein Puffer-Stau), stdout verworfen.
 */
public final class ProcessToolRunner implements ToolRunner {

  /** Erzeugt den Runner. */
  public ProcessToolRunner() {}

  @Override
  public ToolResult run(List<String> command, Duration timeout) throws FrontendException {
    Path stderrFile = null;
    try {
      stderrFile = Files.createTempFile("port-frontend-unity", ".stderr");
      ProcessBuilder builder = new ProcessBuilder(command);
      builder.redirectOutput(ProcessBuilder.Redirect.DISCARD);
      builder.redirectError(stderrFile.toFile());
      Process process = startProcess(builder, command);
      boolean finished = process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
      if (!finished) {
        process.destroyForcibly();
        throw new FrontendException(
            FrontendException.TRANSLATOR_FAILURE,
            "Zeitüberschreitung nach " + timeout + ": " + command);
      }
      String stderr = Files.readString(stderrFile, StandardCharsets.UTF_8);
      return new ToolResult(process.exitValue(), stderr);
    } catch (IOException e) {
      throw new FrontendException(
          FrontendException.GENERAL, "Ein-/Ausgabefehler: " + e.getMessage());
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new FrontendException(FrontendException.GENERAL, "Unterbrochen: " + command);
    } finally {
      deleteQuietly(stderrFile);
    }
  }

  private static Process startProcess(ProcessBuilder builder, List<String> command)
      throws FrontendException {
    try {
      return builder.start();
    } catch (IOException e) {
      throw new FrontendException(
          FrontendException.GENERAL,
          "Programm nicht gefunden oder nicht startbar: "
              + command.get(0)
              + " ("
              + e.getMessage()
              + "). Pfad über -Dport.frontend.unity oder PORT_FRONTEND_UNITY setzen.");
    }
  }

  /** Löscht die Hilfsdatei; schlägt das fehl, bleibt nur eine Datei im Temp-Verzeichnis zurück. */
  private static void deleteQuietly(Path file) {
    if (file == null) {
      return;
    }
    try {
      Files.deleteIfExists(file);
    } catch (IOException e) {
      // Bewusst ignoriert: Aufräumen einer temporären Datei, das Ergebnis ist davon unabhängig.
    }
  }
}
