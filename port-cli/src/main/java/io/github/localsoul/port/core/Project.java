package io.github.localsoul.port.core;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.manifest.GameManifest;
import io.github.localsoul.launcher.manifest.ManifestReader;
import io.github.localsoul.launcher.manifest.ProjectManifest;
import java.nio.file.Path;

/**
 * Ein geöffnetes Spiel-Projekt.
 *
 * @param dir Projektordner
 * @param manifest gelesenes Manifest
 * @param platform Zugriff auf Frontend, Adapter und runtime-system
 */
public record Project(Path dir, GameManifest manifest, PlatformLocator platform) {

  /** Cache- und Zustandsordner. */
  public static final String CACHE = ".port";

  /**
   * Öffnet das Projekt im Projektordner des Kontexts.
   *
   * @param context globale Einstellungen
   * @return Projekt
   * @throws PortException Exit 2 ohne gültiges {@code port.toml} eines Spiels
   */
  public static Project open(CommandContext context) throws PortException {
    ProjectManifest read;
    try {
      read = ManifestReader.read(context.projectDir(), ProjectManifest.FILE);
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
    if (!(read instanceof GameManifest manifest)) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.MANIFEST,
              "die Pipeline gilt für Spiele; Engine-Projekte folgen mit M11a",
              ProjectManifest.FILE,
              "port -C games/<name> ..."));
    }
    PlatformLocator platform = new PlatformLocator(context.projectDir(), context.platformHome());
    return new Project(context.projectDir(), manifest, platform);
  }

  /**
   * Pfad relativ zum Projekt.
   *
   * @param relative z. B. {@code ir}
   * @return absoluter Pfad
   */
  public Path resolve(String relative) {
    return dir.resolve(relative);
  }
}
