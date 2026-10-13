package io.github.localsoul.port.core;

import io.github.localsoul.port.frontend.unity.FrontendConfig;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Findet die Teile der Plattform, die ein Spiel-Projekt braucht (Frontend-Programm, Mappings,
 * Klassen von Adapter und runtime-system). Plattformwurzel ist das Monorepo des Projekts, sonst
 * {@code PORT_HOME}. Übergangslösung bis M4a (Katalog und Profile).
 */
public final class PlatformLocator {

  /** Gebautes Frontend-Programm relativ zur Plattformwurzel ({@code scripts/build-dotnet.sh}). */
  static final String FRONTEND_DLL =
      "frontend-unity/dotnet/src/PortFrontendUnity/bin/Release/net8.0/port-frontend-unity.dll";

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
}
