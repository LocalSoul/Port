package io.github.localsoul.port.core;

import io.github.localsoul.launcher.LauncherException;
import io.github.localsoul.launcher.catalog.Catalog;
import io.github.localsoul.launcher.catalog.CatalogEntry;
import io.github.localsoul.launcher.catalog.Coverage;
import io.github.localsoul.launcher.catalog.Problem;
import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.manifest.GameManifest;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/** {@code port catalog list} und {@code port catalog show} ({@code docs/cli-contract.md}). */
public final class CatalogService {

  private static final List<String> KINDS = List.of("game", "engine", "provider");

  private CatalogService() {}

  /**
   * Wurzel des Monorepos: das des Projektordners, sonst {@code PORT_HOME}.
   *
   * @param context globale Einstellungen
   * @return Wurzel
   * @throws PortException Exit 2, wenn keine bekannt ist
   */
  public static Path root(CommandContext context) throws PortException {
    Path root = Platform.monorepoRoot(context.projectDir());
    if (root == null) {
      root = context.platformHome();
    }
    if (root == null) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.ARGS,
              "kein Monorepo gefunden",
              null,
              "port -C <monorepo> ... oder PORT_HOME setzen"));
    }
    return root;
  }

  /**
   * Listet Einträge, optional nach Art, seitenweise.
   *
   * @param context globale Einstellungen
   * @param kind {@code game}, {@code engine}, {@code provider} oder {@code null}
   * @param limit höchstens so viele Einträge, {@code 0} = alle
   * @param cursor Position aus {@code data.next} eines vorigen Aufrufs oder {@code null}
   * @return Ergebnis mit {@code items}, {@code total}, {@code next}
   * @throws PortException Exit 2 bei ungültiger Art, Grenze oder Position
   */
  public static CommandResult list(CommandContext context, String kind, int limit, String cursor)
      throws PortException {
    if (kind != null && !KINDS.contains(kind)) {
      throw args("unbekannte Art '" + kind + "'", "--kind game, engine oder provider");
    }
    if (limit < 0) {
      throw args("--limit muss >= 0 sein", null);
    }
    int start = position(cursor);
    Catalog catalog = scan(context);
    List<CatalogEntry> all = catalog.list(kind);
    if (start > all.size()) {
      throw args("--cursor hinter dem Ende der Liste", "ohne --cursor neu beginnen");
    }
    int end = limit == 0 ? all.size() : java.lang.Math.min(all.size(), start + limit);
    List<JsonValue> items = new ArrayList<>();
    for (int i = start; i < end; i++) {
      CatalogEntry entry = all.get(i);
      items.add(
          JsonObject.builder()
              .put("name", entry.name())
              .put("kind", entry.kind())
              .put("path", entry.path())
              .build());
    }
    JsonObject data =
        JsonObject.builder()
            .put("items", new JsonValue.JsonArray(items))
            .put("total", all.size())
            .put("next", end < all.size() ? Integer.toString(end) : null)
            .build();
    return CommandResult.ok("catalog list", data, problems(catalog));
  }

  /**
   * Details zu einem Namen.
   *
   * @param context globale Einstellungen
   * @param name Name
   * @param kind Art oder {@code null}
   * @return Ergebnis mit den Feldern des Manifests, bei Engines {@code coverage}
   * @throws PortException Exit 2, wenn der Name fehlt oder mehrdeutig ist
   */
  public static CommandResult show(CommandContext context, String name, String kind)
      throws PortException {
    Catalog catalog = scan(context);
    List<CatalogEntry> matches = new ArrayList<>();
    for (CatalogEntry entry : catalog.find(name)) {
      if (kind == null || entry.kind().equals(kind)) {
        matches.add(entry);
      }
    }
    if (matches.isEmpty()) {
      throw args("nicht im Katalog: " + name, "port catalog list");
    }
    if (matches.size() > 1) {
      throw args(
          "mehrdeutig: " + name + " (" + matches.size() + " Einträge)",
          "--kind game oder --kind engine");
    }
    CatalogEntry entry = matches.get(0);
    JsonObject.Builder data =
        JsonObject.builder()
            .put("name", entry.name())
            .put("kind", entry.kind())
            .put("path", entry.path());
    switch (entry.manifest()) {
      case GameManifest game -> {
        data.put("frontend", game.frontend());
        data.put("source", game.source());
        data.put(
            "requires",
            JsonObject.builder()
                .put("adapter", game.requiresAdapter())
                .put("api", game.requiresApi())
                .put("subsystems", JsonValue.strings(game.subsystems()))
                .build());
      }
      case EngineManifest engine -> {
        data.put(
            "provides",
            JsonObject.builder()
                .put("adapter", engine.adapter())
                .put("api", engine.api())
                .put("coreApi", engine.coreApi())
                .put("providers", JsonValue.strings(engine.providers()))
                .build());
        data.put("module", engine.module());
        Coverage coverage =
            Coverage.of(catalog.root().resolve(entry.path()).resolve(engine.module()));
        data.put(
            "coverage",
            coverage == null
                ? JsonValue.of((String) null)
                : JsonObject.builder()
                    .put("full", coverage.full())
                    .put("stub", coverage.stub())
                    .build());
      }
      case null -> {}
    }
    return CommandResult.ok("catalog show", data.build(), List.of());
  }

  private static Catalog scan(CommandContext context) throws PortException {
    try {
      return Catalog.scan(root(context));
    } catch (LauncherException e) {
      throw LauncherErrors.toPort(e);
    }
  }

  private static List<Diagnostic> problems(Catalog catalog) {
    List<Diagnostic> diagnostics = new ArrayList<>();
    for (Problem problem : catalog.problems()) {
      diagnostics.add(
          new Diagnostic(
              problem.id(),
              Severity.WARN,
              null,
              problem.message(),
              problem.location(),
              problem.hint()));
    }
    return diagnostics;
  }

  private static int position(String cursor) throws PortException {
    if (cursor == null) {
      return 0;
    }
    try {
      int position = Integer.parseInt(cursor);
      if (position < 0) {
        throw args("ungültiger --cursor '" + cursor + "'", null);
      }
      return position;
    } catch (NumberFormatException e) {
      throw args("ungültiger --cursor '" + cursor + "'", "Wert aus data.next verwenden");
    }
  }

  private static PortException args(String message, String hint) {
    return new PortException(
        ExitCode.INVALID, Diagnostic.error(DiagnosticIds.ARGS, message, null, hint));
  }
}
