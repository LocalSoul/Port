package io.github.localsoul.port.core;

import io.github.localsoul.port.generator.java.JavaGenerator;
import io.github.localsoul.port.index.IndexException;
import io.github.localsoul.port.index.IrIndex;
import io.github.localsoul.port.index.RefRow;
import io.github.localsoul.port.index.SymbolRow;
import io.github.localsoul.port.ir.refs.SymbolSource;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Suche und Navigation auf dem Index ({@code docs/cli-contract.md}): {@code find}, {@code refs},
 * {@code callers}, {@code callees}, {@code uses}, {@code show}, {@code outline}. Jeder Aufruf
 * aktualisiert den Index vorher inkrementell (ohne Ausgabe).
 */
public final class SearchService {

  private static final Set<String> TYPE_KINDS = Set.of("class", "struct", "interface", "enum");

  /** Abfrage auf einem aktuellen Index. */
  private interface Query {
    CommandResult run(IrIndex index) throws IndexException, PortException;
  }

  private SearchService() {}

  /**
   * {@code port find <text>}: Symbole, deren Name oder ID den Text enthält.
   *
   * @param context globale Einstellungen
   * @param text Suchtext (ohne Groß/Klein)
   * @param limit {@code --limit}
   * @param cursor {@code --cursor}
   * @return {@code matches}, {@code total}, {@code next}
   * @throws PortException Exit 2 ohne Projekt oder bei ungültiger IR
   */
  public static CommandResult find(CommandContext context, String text, int limit, String cursor)
      throws PortException {
    return query(
        context,
        index -> {
          Page<SymbolRow> page = Page.of(index.find(text), limit, cursor);
          List<JsonValue> matches = new ArrayList<>();
          for (SymbolRow row : page.items()) {
            matches.add(symbol(row));
          }
          return CommandResult.ok("find", paged("matches", matches, page), List.of());
        });
  }

  /**
   * {@code port refs <symbol>}: alle Verwendungen eines Typs oder Members.
   *
   * @param context globale Einstellungen
   * @param id Symbol-ID
   * @param limit {@code --limit}
   * @param cursor {@code --cursor}
   * @return {@code symbol}, {@code items}, {@code total}, {@code next}
   * @throws PortException Exit 2, wenn das Symbol nicht im Projekt deklariert ist
   */
  public static CommandResult refs(CommandContext context, String id, int limit, String cursor)
      throws PortException {
    return query(
        context,
        index -> {
          require(index, id);
          return refList("refs", "symbol", id, index.refs(id), limit, cursor, true);
        });
  }

  /**
   * {@code port callers <methode>}: Symbole, die die Methode aufrufen.
   *
   * @param context globale Einstellungen
   * @param id Symbol-ID der Methode
   * @param limit {@code --limit}
   * @param cursor {@code --cursor}
   * @return {@code method}, {@code items}, {@code total}, {@code next}
   * @throws PortException Exit 2, wenn die Methode nicht im Projekt deklariert ist
   */
  public static CommandResult callers(CommandContext context, String id, int limit, String cursor)
      throws PortException {
    return query(
        context,
        index -> {
          require(index, id);
          return refList("callers", "method", id, index.callers(id), limit, cursor, true);
        });
  }

  /**
   * {@code port callees <methode>}: von der Methode aufgerufene Methoden.
   *
   * @param context globale Einstellungen
   * @param id Symbol-ID der Methode
   * @param limit {@code --limit}
   * @param cursor {@code --cursor}
   * @return {@code method}, {@code items} (mit {@code external}), {@code total}, {@code next}
   * @throws PortException Exit 2, wenn die Methode nicht im Projekt deklariert ist
   */
  public static CommandResult callees(CommandContext context, String id, int limit, String cursor)
      throws PortException {
    return query(
        context,
        index -> {
          require(index, id);
          return refList("callees", "method", id, index.callees(id), limit, cursor, false);
        });
  }

  /**
   * {@code port uses <api>}: Stellen, die eine externe API nutzen (Symbol oder alle Member eines
   * Typs), z. B. {@code UnityEngine.Input}.
   *
   * @param context globale Einstellungen
   * @param api Symbol-ID oder Typname
   * @param limit {@code --limit}
   * @param cursor {@code --cursor}
   * @return {@code api}, {@code items}, {@code total}, {@code next}
   * @throws PortException Exit 2 ohne Projekt
   */
  public static CommandResult uses(CommandContext context, String api, int limit, String cursor)
      throws PortException {
    return query(
        context,
        index -> {
          Page<RefRow> page = Page.of(index.uses(api), limit, cursor);
          List<JsonValue> items = new ArrayList<>();
          for (RefRow row : page.items()) {
            items.add(
                JsonObject.builder()
                    .put("symbol", row.source())
                    .put("api", row.target())
                    .put("location", row.location())
                    .build());
          }
          JsonObject data =
              JsonObject.builder()
                  .put("api", api)
                  .put("items", new JsonValue.JsonArray(items))
                  .put("total", page.total())
                  .put("next", page.next())
                  .build();
          return CommandResult.ok("uses", data, List.of());
        });
  }

  /**
   * {@code port show <symbol>}: Deklaration mit IR-Text; {@code --java} nennt zusätzlich die
   * erzeugte Java-Datei, {@code --src} gibt es noch nicht (C#-Quelle wird nicht aufbewahrt).
   *
   * @param context globale Einstellungen
   * @param id Symbol-ID
   * @param view {@code ir}, {@code java} oder {@code src}
   * @return {@code symbol}, {@code kind}, {@code name}, {@code type}, {@code location}, {@code ir},
   *     {@code irHash} (für {@code // port:override}, ADR-027), {@code javaFile}
   * @throws PortException Exit 2, wenn das Symbol nicht im Projekt deklariert ist
   */
  public static CommandResult show(CommandContext context, String id, String view)
      throws PortException {
    return query(
        context,
        index -> {
          SymbolRow row = require(index, id);
          String ir = index.irText(row);
          List<Diagnostic> diagnostics = new ArrayList<>();
          String javaFile = null;
          if (view.equals("java")) {
            javaFile = javaFile(context, row.type());
            if (javaFile == null) {
              diagnostics.add(
                  Diagnostic.info(DiagnosticIds.TODO, "noch kein Java erzeugt (port gen)"));
            }
          } else if (view.equals("src")) {
            diagnostics.add(
                Diagnostic.info(
                    DiagnosticIds.LATER, "C#-Quelle wird nicht aufbewahrt, --src kommt später"));
          }
          JsonObject data =
              JsonObject.builder()
                  .put("symbol", row.symbol())
                  .put("kind", row.kind())
                  .put("name", row.name())
                  .put("type", row.type())
                  .put("location", row.location())
                  .put("ir", ir)
                  .put("irHash", SymbolSource.sha256(ir))
                  .put("javaFile", javaFile)
                  .build();
          return CommandResult.ok("show", data, diagnostics);
        });
  }

  /**
   * {@code port outline <klasse>}: Mitglieder eines Typs in Deklarationsreihenfolge.
   *
   * @param context globale Einstellungen
   * @param type qualifizierter Typname
   * @return {@code type}, {@code kind}, {@code location}, {@code members}
   * @throws PortException Exit 2, wenn der Typ nicht im Projekt deklariert ist
   */
  public static CommandResult outline(CommandContext context, String type) throws PortException {
    return query(
        context,
        index -> {
          SymbolRow row = require(index, type);
          if (!TYPE_KINDS.contains(row.kind())) {
            throw new PortException(
                ExitCode.INVALID,
                Diagnostic.error(
                    DiagnosticIds.SYMBOL,
                    type + " ist kein Typ (" + row.kind() + ")",
                    row.location(),
                    "port outline " + row.type()));
          }
          List<JsonValue> members = new ArrayList<>();
          for (SymbolRow member : index.outline(type)) {
            members.add(symbol(member));
          }
          JsonObject data =
              JsonObject.builder()
                  .put("type", row.symbol())
                  .put("kind", row.kind())
                  .put("location", row.location())
                  .put("members", new JsonValue.JsonArray(members))
                  .build();
          return CommandResult.ok("outline", data, List.of());
        });
  }

  private static CommandResult query(CommandContext context, Query query) throws PortException {
    Project project = Project.open(context);
    try (IrIndex index = IrIndex.open(project.dir(), false)) {
      index.update();
      return query.run(index);
    } catch (IndexException e) {
      throw IndexErrors.toPort(e);
    }
  }

  private static SymbolRow require(IrIndex index, String id) throws IndexException, PortException {
    SymbolRow row = index.symbol(id);
    if (row == null) {
      String name = id.contains("::") ? id.substring(id.indexOf("::") + 2) : id;
      int end = name.indexOf('#');
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(
              DiagnosticIds.SYMBOL,
              "Symbol nicht im Projekt: " + id,
              null,
              "port find " + (end < 0 ? name : name.substring(0, end))));
    }
    return row;
  }

  private static CommandResult refList(
      String command,
      String key,
      String id,
      List<RefRow> rows,
      int limit,
      String cursor,
      boolean bySource)
      throws PortException {
    Page<RefRow> page = Page.of(rows, limit, cursor);
    List<JsonValue> items = new ArrayList<>();
    for (RefRow row : page.items()) {
      JsonObject.Builder item =
          JsonObject.builder().put("symbol", bySource ? row.source() : row.target());
      if (command.equals("refs")) {
        item.put("kind", row.kind());
      }
      if (!bySource) {
        item.put("external", row.external());
      }
      items.add(item.put("location", row.location()).build());
    }
    JsonObject data =
        JsonObject.builder()
            .put(key, id)
            .put("items", new JsonValue.JsonArray(items))
            .put("total", page.total())
            .put("next", page.next())
            .build();
    return CommandResult.ok(command, data, List.of());
  }

  private static JsonObject paged(String key, List<JsonValue> items, Page<?> page) {
    return JsonObject.builder()
        .put(key, new JsonValue.JsonArray(items))
        .put("total", page.total())
        .put("next", page.next())
        .build();
  }

  private static JsonObject symbol(SymbolRow row) {
    return JsonObject.builder()
        .put("symbol", row.symbol())
        .put("kind", row.kind())
        .put("location", row.location())
        .build();
  }

  /** Erzeugte Datei des Typs ({@code gen/...Base.java}) oder {@code null}. */
  private static String javaFile(CommandContext context, String type) throws PortException {
    Project project = Project.open(context);
    String className =
        JavaGenerator.javaClassName(GenStep.basePackage(project.manifest().name()), type);
    String file = GenStep.DIR + "/" + className.replace('.', '/') + "Base.java";
    return Files.isRegularFile(project.resolve(file)) ? file : null;
  }
}
