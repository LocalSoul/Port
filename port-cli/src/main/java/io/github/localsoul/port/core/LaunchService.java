package io.github.localsoul.port.core;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.catalog.Catalog;
import io.github.localsoul.launcher.catalog.CatalogEntry;
import io.github.localsoul.launcher.launch.LaunchPlan;
import io.github.localsoul.launcher.launch.Launcher;
import io.github.localsoul.launcher.profile.Compatibility;
import io.github.localsoul.launcher.profile.Finding;
import io.github.localsoul.launcher.profile.Profile;
import io.github.localsoul.launcher.profile.ProfileStore;
import io.github.localsoul.port.generator.java.JavaGenerator;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** {@code port launch new/list/check/run} ({@code docs/cli-contract.md}, Katalog und Launcher). */
public final class LaunchService {

  private LaunchService() {}

  /**
   * Listet die Profile.
   *
   * @param context globale Einstellungen
   * @return {@code items} mit {@code name}, {@code game}, {@code engine}; defekte Profile als
   *     Warnung
   * @throws PortException Exit 2 ohne Monorepo
   */
  public static CommandResult list(CommandContext context) throws PortException {
    Path root = CatalogService.root(context);
    List<JsonValue> items = new ArrayList<>();
    List<Diagnostic> diagnostics = new ArrayList<>();
    for (String name : names(root)) {
      try {
        Profile profile = ProfileStore.read(root, name);
        items.add(
            JsonObject.builder()
                .put("name", profile.name())
                .put("game", profile.game())
                .put("engine", profile.engine())
                .build());
      } catch (LauncherException e) {
        diagnostics.add(
            new Diagnostic(e.id(), Severity.WARN, null, e.getMessage(), e.location(), e.hint()));
      }
    }
    JsonObject data = JsonObject.builder().put("items", new JsonValue.JsonArray(items)).build();
    return CommandResult.ok("launch list", data, diagnostics);
  }

  /**
   * Legt ein Profil an.
   *
   * @param context globale Einstellungen
   * @param name Profilname
   * @param game Name des Spiels im Katalog
   * @param engine Name der Engine im Katalog
   * @param sets {@code --set art=provider} (ab M12, hier nur Warnung)
   * @return {@code path} und {@code created}
   * @throws PortException Exit 2 bei unbekanntem Spiel/Engine oder abweichendem vorhandenem Profil
   */
  public static CommandResult create(
      CommandContext context, String name, String game, String engine, List<String> sets)
      throws PortException {
    Path root = CatalogService.root(context);
    try {
      ProfileStore.checkName(name);
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
    Catalog catalog = scan(root);
    CatalogEntry gameEntry = catalog.game(game);
    if (gameEntry == null) {
      throw invalid("Spiel nicht im Katalog: " + game, null, "port catalog list --kind game");
    }
    CatalogEntry engineEntry = catalog.engine(engine);
    if (engineEntry == null) {
      throw invalid("Engine nicht im Katalog: " + engine, null, "port catalog list --kind engine");
    }
    List<Diagnostic> diagnostics = new ArrayList<>();
    if (!sets.isEmpty()) {
      diagnostics.add(
          Diagnostic.warn(
              DiagnosticIds.LATER,
              "--set (Provider-Auswahl) kommt mit M12 und wird ignoriert",
              null));
    }
    Profile profile = Profile.create(name, gameEntry.path(), engineEntry.path());
    Path file = root.resolve(profile.path());
    boolean created;
    try {
      if (Files.exists(file)) {
        if (!Files.readString(file, StandardCharsets.UTF_8).equals(profile.toToml())) {
          throw invalid(
              "Profil existiert bereits mit anderem Inhalt",
              profile.path(),
              "anderen Namen wählen oder " + profile.path() + " von Hand anpassen");
        }
        diagnostics.add(
            Diagnostic.info(DiagnosticIds.EXISTS, "Profil existiert bereits, unverändert"));
      }
      created = ProjectFiles.writeAtomic(file, profile.toToml());
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL,
          Diagnostic.error(DiagnosticIds.IO, e.getMessage(), profile.path(), null));
    }
    diagnostics.add(
        Diagnostic.info(
            DiagnosticIds.PROFILE,
            "[run] spawn im Profil eintragen (IR-Typen, die beim Start ein GameObject bekommen)"));
    JsonObject data =
        JsonObject.builder().put("path", profile.path()).put("created", created).build();
    return CommandResult.ok("launch new", data, diagnostics);
  }

  /**
   * Prüft ein Profil.
   *
   * @param context globale Einstellungen
   * @param name Profilname
   * @return {@code compatible} und {@code checks}; Fehlschläge als Fehler mit nächstem Befehl, dann
   *     Exit 2
   * @throws PortException Exit 2, wenn das Profil fehlt oder ungültig ist
   */
  public static CommandResult check(CommandContext context, String name) throws PortException {
    Path root = CatalogService.root(context);
    Profile profile = read(root, name);
    List<Finding> findings = Compatibility.check(profile, scan(root));
    List<JsonValue> checks = new ArrayList<>();
    List<Diagnostic> diagnostics = new ArrayList<>();
    for (Finding finding : findings) {
      checks.add(
          JsonObject.builder()
              .put("check", finding.check())
              .put("ok", finding.ok())
              .put("message", finding.message())
              .build());
      if (!finding.ok()) {
        diagnostics.add(
            Diagnostic.error(
                DiagnosticIds.PROFILE, finding.message(), finding.location(), finding.hint()));
      }
    }
    boolean compatible = Compatibility.compatible(findings);
    JsonObject data =
        JsonObject.builder()
            .put("profile", profile.name())
            .put("game", profile.game())
            .put("engine", profile.engine())
            .put("compatible", compatible)
            .put("checks", new JsonValue.JsonArray(checks))
            .build();
    if (!compatible) {
      return CommandResult.failed("launch check", data, diagnostics, ExitCode.INVALID);
    }
    return CommandResult.ok("launch check", data, diagnostics);
  }

  /**
   * Startet ein Profil: prüfen, Spiel bei Bedarf bauen, Engine und Plattform prüfen, Spiel-JVM mit
   * {@code launcher-boot} starten. Die Ausgabe des Spiels geht auf stdout (mit {@code --json} auf
   * stderr).
   *
   * @param context globale Einstellungen
   * @param name Profilname
   * @param forceBuild {@code --build}
   * @return {@code profile}, {@code built}, {@code gameExitCode}; Exit-Code des Spiels als Ergebnis
   *     (Codes außerhalb des Vertrags werden 1)
   * @throws PortException Exit 2 bei unpassendem Profil, Exit 8 bei ungebauter Plattform, Exit des
   *     Baus bei Baufehlern
   */
  public static CommandResult run(CommandContext context, String name, boolean forceBuild)
      throws PortException {
    CommandResult check = check(context, name);
    if (check.failure() != null) {
      return CommandResult.failed("launch run", check.data(), check.diagnostics(), check.failure());
    }
    Path root = CatalogService.root(context);
    Profile profile = read(root, name);
    Catalog catalog = scan(root);
    CatalogEntry game = null;
    for (CatalogEntry entry : catalog.list("game")) {
      if (entry.path().equals(profile.game())) {
        game = entry;
      }
    }
    List<Diagnostic> diagnostics = new ArrayList<>();
    if (profile.spawn().isEmpty()) {
      diagnostics.add(
          Diagnostic.warn(
              DiagnosticIds.PROFILE,
              "[run] spawn ist leer, es wird nichts angelegt",
              profile.path()));
    }
    String basePackage = GenStep.basePackage(game.name());
    List<String> classNames = new ArrayList<>();
    for (String type : profile.spawn()) {
      classNames.add(JavaGenerator.javaClassName(basePackage, type));
    }
    PortGameBuilder builder = new PortGameBuilder(context);
    LaunchPlan plan;
    try {
      plan = Launcher.prepare(catalog, profile, classNames, forceBuild, builder);
    } catch (LauncherException e) {
      diagnostics.addAll(builder.diagnostics());
      diagnostics.add(Diagnostic.error(e.id(), e.getMessage(), e.location(), e.hint()));
      return CommandResult.failed(
          "launch run", JsonObject.empty(), diagnostics, ExitCode.of(e.exitCode()));
    }
    diagnostics.addAll(builder.diagnostics());
    context.progress("port: starte " + String.join(" ", plan.command()));
    int gameExit;
    try {
      gameExit =
          Launcher.run(
              plan,
              root.resolve(profile.game()),
              context.json() ? context.log() : context.out(),
              context.log());
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
    JsonObject data =
        JsonObject.builder()
            .put("profile", profile.name())
            .put("built", plan.built())
            .put("gameExitCode", gameExit)
            .build();
    if (gameExit != 0) {
      diagnostics.add(
          Diagnostic.error(
              DiagnosticIds.RUN, "Spiel endete mit Exit " + gameExit, profile.path(), null));
      return CommandResult.failed("launch run", data, diagnostics, ExitCode.of(gameExit));
    }
    return CommandResult.ok("launch run", data, diagnostics);
  }

  static Profile read(Path root, String name) throws PortException {
    try {
      return ProfileStore.read(root, name);
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
  }

  static Catalog scan(Path root) throws PortException {
    try {
      return Catalog.scan(root);
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
  }

  private static List<String> names(Path root) throws PortException {
    try {
      return ProfileStore.names(root);
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
  }

  private static PortException invalid(String message, String location, String hint) {
    return new PortException(
        ExitCode.INVALID, Diagnostic.error(DiagnosticIds.PROFILE, message, location, hint));
  }
}
