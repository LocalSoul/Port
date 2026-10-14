package io.github.localsoul.launcher.profile;

import io.github.localsoul.launcher.catalog.Catalog;
import io.github.localsoul.launcher.catalog.CatalogEntry;
import io.github.localsoul.launcher.manifest.CoreApi;
import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.manifest.GameManifest;
import io.github.localsoul.launcher.manifest.VersionRange;
import java.util.ArrayList;
import java.util.List;

/**
 * {@code port launch check}: Spiel und Engine vorhanden, Adapter-Name, API-Version im geforderten
 * Bereich, Engine-API der Plattform im Bereich der Engine, Core-Implementierung. Provider-Prüfung
 * kommt mit M12 (ADR-015), bis dahin gelten die Standard-Provider der Engine.
 */
public final class Compatibility {

  private Compatibility() {}

  /**
   * Prüft ein Profil gegen den Katalog.
   *
   * @param profile Profil
   * @param catalog Katalog
   * @return Ergebnisse in fester Reihenfolge; abhängige Prüfungen entfallen, wenn Spiel oder Engine
   *     fehlen
   */
  public static List<Finding> check(Profile profile, Catalog catalog) {
    List<Finding> findings = new ArrayList<>();
    String file = profile.path();
    GameManifest game = game(profile, catalog, findings, file);
    EngineManifest engine = engine(profile, catalog, findings, file);
    findings.add(
        profile.core().equals(Profile.CORE)
            ? ok("core", "Core " + Profile.CORE)
            : fail(
                "core",
                "unbekannte Core-Implementierung '" + profile.core() + "'",
                file,
                "core = \"" + Profile.CORE + "\" im Profil eintragen"));
    if (game == null || engine == null) {
      return findings;
    }
    String alternative = "port catalog list --kind engine";
    if (game.requiresAdapter().equals(engine.adapter())) {
      findings.add(ok("adapter", "Adapter " + engine.adapter()));
    } else {
      findings.add(
          fail(
              "adapter",
              "Spiel braucht Adapter '"
                  + game.requiresAdapter()
                  + "', Engine stellt '"
                  + engine.adapter()
                  + "' bereit",
              file,
              alternative));
    }
    if (VersionRange.parse(game.requiresApi()).contains(engine.api())) {
      findings.add(ok("api", "API " + engine.api() + " erfüllt " + game.requiresApi()));
    } else {
      findings.add(
          fail(
              "api",
              "API " + engine.api() + " der Engine liegt nicht in " + game.requiresApi(),
              file,
              alternative));
    }
    if (VersionRange.parse(engine.coreApi()).contains(CoreApi.version())) {
      findings.add(
          ok("core-api", "Engine-API " + CoreApi.version() + " erfüllt " + engine.coreApi()));
    } else {
      findings.add(
          fail(
              "core-api",
              "Engine-API "
                  + CoreApi.version()
                  + " liegt nicht in core-api "
                  + engine.coreApi()
                  + " der Engine",
              profile.engine() + "/port.toml",
              "core-api der Engine anpassen"));
    }
    return findings;
  }

  /**
   * {@code true}, wenn alle Prüfungen bestanden sind.
   *
   * @param findings Ergebnisse
   * @return Gesamtergebnis
   */
  public static boolean compatible(List<Finding> findings) {
    for (Finding finding : findings) {
      if (!finding.ok()) {
        return false;
      }
    }
    return true;
  }

  private static GameManifest game(
      Profile profile, Catalog catalog, List<Finding> findings, String file) {
    for (CatalogEntry entry : catalog.list("game")) {
      if (entry.path().equals(profile.game()) && entry.manifest() instanceof GameManifest game) {
        findings.add(ok("game", "Spiel " + entry.name()));
        return game;
      }
    }
    findings.add(
        fail(
            "game",
            "Spiel nicht gefunden: " + profile.game(),
            file,
            "port catalog list --kind game"));
    return null;
  }

  private static EngineManifest engine(
      Profile profile, Catalog catalog, List<Finding> findings, String file) {
    for (CatalogEntry entry : catalog.list("engine")) {
      if (entry.path().equals(profile.engine())
          && entry.manifest() instanceof EngineManifest engine) {
        findings.add(ok("engine", "Engine " + entry.name()));
        return engine;
      }
    }
    findings.add(
        fail(
            "engine",
            "Engine nicht gefunden: " + profile.engine(),
            file,
            "port catalog list --kind engine"));
    return null;
  }

  private static Finding ok(String check, String message) {
    return new Finding(check, true, message, null, null);
  }

  private static Finding fail(String check, String message, String location, String hint) {
    return new Finding(check, false, message, location, hint);
  }
}
