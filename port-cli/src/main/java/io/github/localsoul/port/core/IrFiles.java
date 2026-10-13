package io.github.localsoul.port.core;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.reader.IrReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** Liest {@code ir/*.ir} eines Projekts. */
final class IrFiles {

  private IrFiles() {}

  /**
   * Liest alle IR-Dateien, sortiert nach Dateiname.
   *
   * @throws PortException Exit 2 bei Syntaxfehler (mit Ort), Exit 1 bei Lesefehler
   */
  static List<IrFile> read(Path projectDir) throws PortException {
    Path dir = projectDir.resolve(IrStep.DIR);
    List<Path> paths = new ArrayList<>();
    try {
      if (Files.isDirectory(dir)) {
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.ir")) {
          for (Path path : stream) {
            paths.add(path);
          }
        }
      }
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL, Diagnostic.error(DiagnosticIds.IO, e.getMessage(), IrStep.DIR, null));
    }
    paths.sort(null);
    List<IrFile> files = new ArrayList<>();
    IrReader reader = new IrReader();
    for (Path path : paths) {
      String name = IrStep.DIR + "/" + path.getFileName();
      try {
        files.add(reader.read(Files.readString(path, StandardCharsets.UTF_8), name));
      } catch (IrSyntaxException e) {
        throw new PortException(
            ExitCode.INVALID,
            Diagnostic.error(
                DiagnosticIds.IR, e.getMessage(), e.span().toString(), "port ir neu ausführen"));
      } catch (IOException e) {
        throw new PortException(
            ExitCode.GENERAL, Diagnostic.error(DiagnosticIds.IO, e.getMessage(), name, null));
      }
    }
    return files;
  }
}
