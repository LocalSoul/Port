package io.github.localsoul.port.frontend.unity;

import java.nio.file.Files;
import java.nio.file.Path;

/** Pfade im Repo für Integrationstests (Arbeitsverzeichnis ist {@code frontend-unity/java}). */
final class RepoPaths {

  static final Path ROOT = Path.of("..", "..").toAbsolutePath().normalize();

  static final Path TOOL_DLL =
      ROOT.resolve(
          "frontend-unity/dotnet/src/PortFrontendUnity/bin/Release/net8.0/port-frontend-unity.dll");

  static final Path FIXTURE_DLL =
      ROOT.resolve("testdata/unity-sources/bin/Release/netstandard2.1/Assembly-CSharp.dll");

  static final Path GOLDEN_DIR = ROOT.resolve("testdata/golden/ir/unity");

  private RepoPaths() {}

  /** {@code true}, wenn das gebaute Programm und {@code dotnet} vorhanden sind. */
  static boolean toolAvailable() {
    return Files.exists(TOOL_DLL) && dotnetOnPath();
  }

  private static boolean dotnetOnPath() {
    String path = System.getenv("PATH");
    if (path == null) {
      return false;
    }
    for (String entry : path.split(java.io.File.pathSeparator)) {
      if (Files.isExecutable(Path.of(entry, "dotnet"))) {
        return true;
      }
    }
    return false;
  }
}
