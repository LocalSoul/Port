package io.github.localsoul.port.cli;

import java.nio.file.Files;
import java.nio.file.Path;

/** Synthetisches Testspiel und gebautes Frontend ({@code scripts/build-dotnet.sh}). */
final class Fixtures {

  static final Path ASSEMBLY =
      Golden.ROOT.resolve("testdata/unity-sources/bin/Release/netstandard2.1/Assembly-CSharp.dll");

  static final Path FRONTEND_DLL =
      Golden.ROOT.resolve(
          "frontend-unity/dotnet/src/PortFrontendUnity/bin/Release/net8.0/port-frontend-unity.dll");

  static final Path IR_GOLDEN = Golden.ROOT.resolve("testdata/golden/ir/unity");

  static final String SKIP = "Frontend oder Fixture nicht gebaut: scripts/build-dotnet.sh";

  private Fixtures() {}

  /** {@code true}, wenn Fixture-DLL, gebautes Frontend und {@code dotnet} vorhanden sind. */
  static boolean frontendAvailable() {
    return Files.isRegularFile(ASSEMBLY) && Files.isRegularFile(FRONTEND_DLL) && onPath("dotnet");
  }

  /** {@code true}, wenn {@code git} im {@code PATH} liegt. */
  static boolean gitAvailable() {
    return onPath("git");
  }

  /** Führt {@code git} aus und liefert die Ausgabe (für Prüfungen in Tests). */
  static String git(Path dir, String... args) throws Exception {
    java.util.List<String> command = new java.util.ArrayList<>();
    command.add("git");
    command.addAll(java.util.List.of(args));
    Process process =
        new ProcessBuilder(command).directory(dir.toFile()).redirectErrorStream(true).start();
    String output =
        new String(
            process.getInputStream().readAllBytes(), java.nio.charset.StandardCharsets.UTF_8);
    process.waitFor();
    return output;
  }

  static boolean onPath(String program) {
    String path = System.getenv("PATH");
    if (path == null) {
      return false;
    }
    for (String entry : path.split(java.io.File.pathSeparator)) {
      if (Files.isExecutable(Path.of(entry, program))) {
        return true;
      }
    }
    return false;
  }
}
