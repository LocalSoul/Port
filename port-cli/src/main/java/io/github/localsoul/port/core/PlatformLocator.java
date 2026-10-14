package io.github.localsoul.port.core;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.manifest.ManifestReader;
import io.github.localsoul.launcher.manifest.ProjectManifest;
import io.github.localsoul.port.frontend.unity.FrontendConfig;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Findet die Teile der Plattform, die ein Spiel-Projekt braucht (Frontend-Programm, Mappings,
 * Klassen von Adapter und runtime-system). Plattformwurzel ist das Monorepo des Projekts, sonst
 * {@code PORT_HOME}. Übergangslösung bis M4a (Katalog und Profile).
 */
public final class PlatformLocator {

  /** Gebautes Frontend-Programm relativ zur Plattformwurzel ({@code scripts/build-dotnet.sh}). */
  static final String FRONTEND_DLL =
      "frontend-unity/dotnet/src/PortFrontendUnity/bin/Release/net8.0/port-frontend-unity.dll";

  /** Mapping eines Moduls relativ zum Modulordner. */
  static final String MAPPING = "src/main/resources/mapping.tsv";

  private final Path root;

  /**
   * Erzeugt den Locator.
   *
   * @param projectDir Projektordner
   * @param platformHome {@code PORT_HOME} oder {@code null}
   */
  public PlatformLocator(Path projectDir, Path platformHome) {
    Path monorepo = Platform.monorepoRoot(projectDir);
    this.root = monorepo != null ? monorepo : platformHome;
  }

  /**
   * Plattformwurzel.
   *
   * @return Wurzel oder {@code null}, wenn weder Monorepo noch {@code PORT_HOME} bekannt sind
   */
  public Path root() {
    return root;
  }

  /**
   * Startbefehl des Unity-Frontends: ausdrücklich konfiguriert ({@code port.frontend.unity} bzw.
   * {@code PORT_FRONTEND_UNITY}), sonst das gebaute Programm der Plattform, sonst {@code
   * port-frontend-unity} im {@code PATH}.
   *
   * @return Konfiguration
   */
  public FrontendConfig frontend() {
    if (configured(System.getProperty(FrontendConfig.PROPERTY))
        || configured(System.getenv(FrontendConfig.ENVIRONMENT))) {
      return FrontendConfig.fromEnvironment();
    }
    if (root != null && Files.isRegularFile(root.resolve(FRONTEND_DLL))) {
      return FrontendConfig.forDll(root.resolve(FRONTEND_DLL));
    }
    return FrontendConfig.fromEnvironment();
  }

  private static boolean configured(String value) {
    return value != null && !value.isBlank();
  }

  /**
   * Maven-Modul des Adapters. {@code --adapter} darf ein Engine-Ordner (mit {@code port.toml}) oder
   * direkt das Modul sein; ohne Angabe gilt {@code engines/<name>/port.toml} der Plattform, dessen
   * {@code [engine.build] module} das Modul nennt.
   *
   * @param override {@code --adapter} oder {@code null}
   * @param adapterName {@code game.requires.adapter}
   * @return vorhandener Modulordner
   * @throws PortException Exit 8, wenn Engine oder Modul fehlen; Exit 2 bei ungültigem Manifest
   */
  public Path adapter(Path override, String adapterName) throws PortException {
    Path engineDir =
        override != null ? override : requireRoot().resolve("engines").resolve(adapterName);
    if (Files.isRegularFile(engineDir.resolve(ProjectManifest.FILE))) {
      return moduleOf(engineDir, adapterName);
    }
    if (override != null && Files.isRegularFile(override.resolve(MAPPING))) {
      return override;
    }
    throw missing(
        "Engine nicht gefunden: " + engineDir + " (kein port.toml)",
        "--adapter <engine-ordner> angeben oder port init --kind engine " + adapterName);
  }

  private static Path moduleOf(Path engineDir, String adapterName) throws PortException {
    ProjectManifest manifest;
    try {
      manifest =
          ManifestReader.read(engineDir, engineDir.getFileName() + "/" + ProjectManifest.FILE);
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
    if (!(manifest instanceof EngineManifest engine)) {
      throw missing("kein Engine-Projekt: " + engineDir, null);
    }
    if (!engine.adapter().equals(adapterName)) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.MANIFEST,
              "Engine stellt '"
                  + engine.adapter()
                  + "' bereit, das Spiel braucht '"
                  + adapterName
                  + "'",
              null,
              "passende Engine mit --adapter angeben"));
    }
    Path module = engineDir.resolve(engine.module());
    if (!Files.isDirectory(module)) {
      throw missing("Modul der Engine fehlt: " + module, null);
    }
    return module;
  }

  /**
   * Mapping-Dateien von Adapter und runtime-system.
   *
   * @param adapter Adapter-Ordner
   * @return zwei vorhandene Dateien
   * @throws PortException Exit 8, wenn eine fehlt
   */
  public List<Path> mappings(Path adapter) throws PortException {
    List<Path> files = new ArrayList<>();
    files.add(adapter.resolve(MAPPING));
    files.add(requireRoot().resolve("runtime-system/src/main/resources/mapping.tsv"));
    for (Path file : files) {
      if (!Files.isRegularFile(file)) {
        throw missing("Mapping nicht gefunden: " + file, null);
      }
    }
    return files;
  }

  /**
   * Klassenpfad für {@code build}: kompilierte Klassen von Adapter, runtime-system, engine-api und
   * engine-core.
   *
   * @param adapter Adapter-Ordner
   * @return vorhandene Ordner
   * @throws PortException Exit 8, wenn die Plattform nicht gebaut ist
   */
  public List<Path> classpath(Path adapter) throws PortException {
    Path root = requireRoot();
    List<Path> entries = new ArrayList<>();
    entries.add(adapter.resolve("target/classes"));
    entries.add(root.resolve("runtime-system/target/classes"));
    entries.add(root.resolve("engine-api/target/classes"));
    entries.add(root.resolve("engine-core/target/classes"));
    for (Path entry : entries) {
      if (!Files.isDirectory(entry)) {
        throw missing("Plattform nicht gebaut: " + entry + " fehlt", "./mvnw -q install");
      }
    }
    return entries;
  }

  private Path requireRoot() throws PortException {
    if (root == null) {
      throw missing(
          "Plattform unbekannt: Projekt liegt nicht im Monorepo und PORT_HOME ist nicht gesetzt",
          "PORT_HOME auf das Recomp-Repository setzen");
    }
    return root;
  }

  private static PortException missing(String message, String hint) {
    return new PortException(
        ExitCode.DEPENDENCY, Diagnostic.error(DiagnosticIds.PLATFORM, message, null, hint));
  }
}
