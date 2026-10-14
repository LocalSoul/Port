package io.github.localsoul.launcher.catalog;

import io.github.localsoul.launcher.manifest.ProjectManifest;

/**
 * Ein Eintrag des Katalogs.
 *
 * @param kind {@code game}, {@code engine} oder {@code provider}
 * @param name Name (bei Providern der Ordnername)
 * @param path Ordner relativ zur Wurzel, z. B. {@code games/demo}
 * @param manifest Manifest bei Spielen und Engines, {@code null} bei Providern
 */
public record CatalogEntry(String kind, String name, String path, ProjectManifest manifest) {}
