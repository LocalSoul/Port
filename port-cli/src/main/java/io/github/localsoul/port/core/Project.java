package io.github.localsoul.port.core;

import java.nio.file.Path;

/**
 * Ein geöffnetes Spiel-Projekt.
 *
 * @param dir Projektordner
 * @param manifest gelesenes Manifest
 * @param platform Zugriff auf Frontend, Adapter und runtime-system
 */
public record Project(Path dir, Manifest manifest, PlatformLocator platform) {

  /** Cache- und Zustandsordner. */
  public static final String CACHE = ".port";

  /**
   * Öffnet das Projekt im Projektordner des Kontexts.
   *
   * @param context globale Einstellungen
   * @return Projekt
   * @throws PortException Exit 2 ohne gültiges {@code port.toml}
   */
  public static Project open(CommandContext context) throws PortException {
    Manifest manifest = ManifestReader.read(context.projectDir());
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
