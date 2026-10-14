package io.github.localsoul.port.index;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.atom.ArrayType;
import io.github.localsoul.port.ir.atom.ByRefType;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.NullableType;
import io.github.localsoul.port.ir.atom.PrimitiveType;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.reader.IrReader;
import io.github.localsoul.port.ir.refs.Declaration;
import io.github.localsoul.port.ir.refs.Declarations;
import io.github.localsoul.port.ir.refs.Reference;
import io.github.localsoul.port.ir.refs.References;
import io.github.localsoul.port.ir.refs.SymbolSource;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Index über {@code ir/*.ir} eines Projekts in {@code .port/index.db} (SQLite, {@code
 * docs/modules/index.md}). Aktualisiert inkrementell je Datei (SHA-256), in einer Transaktion. Alle
 * Abfragen sind deterministisch sortiert.
 */
public final class IrIndex implements AutoCloseable {

  /** Datei des Index relativ zum Projekt. */
  public static final String FILE = ".port/index.db";

  /** Version des Schemas; bei Abweichung wird der Index neu aufgebaut. */
  static final String SCHEMA_VERSION = "1";

  private static final String IR_DIR = "ir";

  private final Path projectDir;
  private final Connection connection;

  private IrIndex(Path projectDir, Connection connection) {
    this.projectDir = projectDir;
    this.connection = connection;
  }

  /**
   * Öffnet (und legt bei Bedarf an) den Index eines Projekts.
   *
   * @param projectDir Projektordner
   * @param rebuild vorhandenen Index verwerfen
   * @return geöffneter Index, mit {@link #close()} schließen
   * @throws IndexException Exit 1, wenn die Datenbank nicht geöffnet werden kann
   */
  public static IrIndex open(Path projectDir, boolean rebuild) throws IndexException {
    Path file = projectDir.resolve(FILE);
    try {
      Files.createDirectories(file.getParent());
      if (rebuild) {
        Files.deleteIfExists(file);
      }
      Connection connection = DriverManager.getConnection("jdbc:sqlite:" + file.toAbsolutePath());
      IrIndex index = new IrIndex(projectDir, connection);
      index.ensureSchema();
      return index;
    } catch (IOException | SQLException e) {
      throw new IndexException(
          IndexException.GENERAL, "Index nicht nutzbar: " + e.getMessage(), FILE, e);
    }
  }

  @Override
  public void close() throws IndexException {
    try {
      connection.close();
    } catch (SQLException e) {
      throw error(e);
    }
  }

  private void ensureSchema() throws SQLException {
    try (Statement statement = connection.createStatement()) {
      statement.execute(
          "CREATE TABLE IF NOT EXISTS meta(key TEXT PRIMARY KEY, value TEXT NOT NULL)");
      String version = null;
      try (ResultSet rows = statement.executeQuery("SELECT value FROM meta WHERE key = 'schema'")) {
        if (rows.next()) {
          version = rows.getString(1);
        }
      }
      if (SCHEMA_VERSION.equals(version)) {
        return;
      }
      for (String table : new String[] {"files", "symbols", "refs", "changes", "symbols_fts"}) {
        statement.execute("DROP TABLE IF EXISTS " + table);
      }
      statement.execute("CREATE TABLE files(path TEXT PRIMARY KEY, sha256 TEXT NOT NULL)");
      statement.execute(
          "CREATE TABLE symbols(id TEXT NOT NULL, kind TEXT NOT NULL, name TEXT NOT NULL,"
              + " type TEXT NOT NULL, file TEXT NOT NULL, line INTEGER NOT NULL,"
              + " col INTEGER NOT NULL, hash TEXT NOT NULL)");
      statement.execute("CREATE INDEX symbols_id ON symbols(id)");
      statement.execute("CREATE INDEX symbols_type ON symbols(type)");
      statement.execute("CREATE INDEX symbols_file ON symbols(file)");
      statement.execute(
          "CREATE TABLE refs(source TEXT NOT NULL, target TEXT NOT NULL, kind TEXT NOT NULL,"
              + " file TEXT NOT NULL, line INTEGER NOT NULL, col INTEGER NOT NULL)");
      statement.execute("CREATE INDEX refs_target ON refs(target)");
      statement.execute("CREATE INDEX refs_source ON refs(source)");
      statement.execute("CREATE INDEX refs_file ON refs(file)");
      statement.execute("CREATE TABLE changes(symbol TEXT PRIMARY KEY, change TEXT NOT NULL)");
      statement.execute(
          "CREATE VIRTUAL TABLE symbols_fts USING fts5(id, name, file UNINDEXED, tokenize = 'trigram')");
      statement.execute(
          "INSERT OR REPLACE INTO meta(key, value) VALUES ('schema', '" + SCHEMA_VERSION + "')");
    }
  }

  // --- Aktualisierung ---

  /**
   * Liest neue und geänderte IR-Dateien ein und entfernt verschwundene. Hat sich mindestens eine
   * Datei geändert, ersetzt der Lauf die gespeicherten Änderungen je Symbol ({@link #changes()}).
   *
   * @return was sich geändert hat
   * @throws IndexException Exit 2 bei ungültiger IR (mit Ort), Exit 1 bei Lese- oder DB-Fehlern
   */
  public UpdateResult update() throws IndexException {
    Map<String, String> current = currentFiles();
    try {
      Map<String, String> indexed = indexedFiles();
      List<String> added = new ArrayList<>();
      List<String> changed = new ArrayList<>();
      List<String> removed = new ArrayList<>();
      for (Map.Entry<String, String> file : current.entrySet()) {
        String old = indexed.get(file.getKey());
        if (old == null) {
          added.add(file.getKey());
        } else if (!old.equals(file.getValue())) {
          changed.add(file.getKey());
        }
      }
      for (String path : indexed.keySet()) {
        if (!current.containsKey(path)) {
          removed.add(path);
        }
      }
      List<Change> changes = List.of();
      if (!added.isEmpty() || !changed.isEmpty() || !removed.isEmpty()) {
        changes = apply(current, added, changed, removed);
      }
      return new UpdateResult(
          added, changed, removed, count("SELECT COUNT(*) FROM symbols"), changes);
    } catch (SQLException e) {
      throw error(e);
    }
  }

  private List<Change> apply(
      Map<String, String> current, List<String> added, List<String> changed, List<String> removed)
      throws SQLException, IndexException {
    Map<String, String> before = symbolHashes();
    connection.setAutoCommit(false);
    try {
      List<String> stale = new ArrayList<>(changed);
      stale.addAll(removed);
      for (String path : stale) {
        deleteFile(path);
      }
      List<String> fresh = new ArrayList<>(added);
      fresh.addAll(changed);
      fresh.sort(null);
      for (String path : fresh) {
        insertFile(path, current.get(path));
      }
      Map<String, String> after = symbolHashes();
      List<Change> changes = diff(before, after);
      try (Statement statement = connection.createStatement()) {
        statement.execute("DELETE FROM changes");
      }
      try (PreparedStatement insert =
          connection.prepareStatement("INSERT INTO changes(symbol, change) VALUES (?, ?)")) {
        for (Change change : changes) {
          insert.setString(1, change.symbol());
          insert.setString(2, change.change());
          insert.executeUpdate();
        }
      }
      connection.commit();
      return changes;
    } catch (SQLException | IndexException | RuntimeException e) {
      connection.rollback();
      throw e;
    } finally {
      connection.setAutoCommit(true);
    }
  }

  private static List<Change> diff(Map<String, String> before, Map<String, String> after) {
    Set<String> ids = new TreeSet<>(before.keySet());
    ids.addAll(after.keySet());
    List<Change> changes = new ArrayList<>();
    for (String id : ids) {
      String old = before.get(id);
      String now = after.get(id);
      if (old == null) {
        changes.add(new Change(id, "added"));
      } else if (now == null) {
        changes.add(new Change(id, "removed"));
      } else if (!old.equals(now)) {
        changes.add(new Change(id, "changed"));
      }
    }
    return changes;
  }

  private void deleteFile(String path) throws SQLException {
    for (String sql :
        new String[] {
          "DELETE FROM files WHERE path = ?",
          "DELETE FROM symbols WHERE file = ?",
          "DELETE FROM refs WHERE file = ?",
          "DELETE FROM symbols_fts WHERE file = ?"
        }) {
      try (PreparedStatement statement = connection.prepareStatement(sql)) {
        statement.setString(1, path);
        statement.executeUpdate();
      }
    }
  }

  private void insertFile(String path, String sha256) throws SQLException, IndexException {
    String text = read(path);
    IrFile file;
    try {
      file = new IrReader().read(text, path);
    } catch (IrSyntaxException e) {
      throw new IndexException(IndexException.INVALID, e.getMessage(), e.span().toString(), e);
    }
    try (PreparedStatement insert =
        connection.prepareStatement("INSERT INTO files(path, sha256) VALUES (?, ?)")) {
      insert.setString(1, path);
      insert.setString(2, sha256);
      insert.executeUpdate();
    }
    insertSymbols(path, text, Declarations.collect(file));
    insertRefs(path, References.collect(file));
  }

  private void insertSymbols(String path, String text, List<Declaration> declarations)
      throws SQLException {
    try (PreparedStatement symbols =
            connection.prepareStatement(
                "INSERT INTO symbols(id, kind, name, type, file, line, col, hash) VALUES (?, ?, ?, ?, ?, ?, ?, ?)");
        PreparedStatement fts =
            connection.prepareStatement(
                "INSERT INTO symbols_fts(id, name, file) VALUES (?, ?, ?)")) {
      for (Declaration declaration : declarations) {
        SourceSpan span = declaration.span();
        String slice = SymbolSource.slice(text, span.line(), span.column());
        symbols.setString(1, declaration.symbol());
        symbols.setString(2, declaration.kind());
        symbols.setString(3, declaration.name());
        symbols.setString(4, declaration.type());
        symbols.setString(5, path);
        symbols.setInt(6, span.line());
        symbols.setInt(7, span.column());
        symbols.setString(8, SymbolSource.sha256(slice));
        symbols.executeUpdate();
        fts.setString(1, declaration.symbol());
        fts.setString(2, declaration.name());
        fts.setString(3, path);
        fts.executeUpdate();
      }
    }
  }

  private void insertRefs(String path, List<Reference> references) throws SQLException {
    try (PreparedStatement insert =
        connection.prepareStatement(
            "INSERT INTO refs(source, target, kind, file, line, col) VALUES (?, ?, ?, ?, ?, ?)")) {
      for (Reference reference : references) {
        List<String[]> targets = new ArrayList<>();
        if (reference.member() != null) {
          String id = reference.member().symbolId();
          targets.add(new String[] {id, id.contains("#") ? "call" : "member"});
        } else {
          for (String type : namedTypes(reference.type(), reference.genericScope())) {
            targets.add(new String[] {type, "type"});
          }
        }
        for (String[] target : targets) {
          insert.setString(1, reference.symbol());
          insert.setString(2, target[0]);
          insert.setString(3, target[1]);
          insert.setString(4, path);
          insert.setInt(5, reference.span().line());
          insert.setInt(6, reference.span().column());
          insert.executeUpdate();
        }
      }
    }
  }

  /** Benannte Typen in einer Typreferenz, ohne Typparameter und {@code unknown}. */
  private static List<String> namedTypes(TypeRef type, Set<String> genericScope) {
    List<String> result = new ArrayList<>();
    switch (type) {
      case PrimitiveType p -> {}
      case ArrayType a -> result.addAll(namedTypes(a.elementType(), genericScope));
      case NullableType n -> result.addAll(namedTypes(n.underlyingType(), genericScope));
      case ByRefType b -> result.addAll(namedTypes(b.referencedType(), genericScope));
      case NamedType n -> {
        String name = n.qualifiedName();
        if (!genericScope.contains(name) && !name.equals("unknown")) {
          result.add(name);
        }
        for (TypeRef argument : n.typeArguments()) {
          result.addAll(namedTypes(argument, genericScope));
        }
      }
    }
    return result;
  }

  private Map<String, String> currentFiles() throws IndexException {
    Map<String, String> files = new TreeMap<>();
    Path dir = projectDir.resolve(IR_DIR);
    if (!Files.isDirectory(dir)) {
      return files;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.ir")) {
      for (Path path : stream) {
        String relative = IR_DIR + "/" + path.getFileName();
        files.put(relative, SymbolSource.sha256(Files.readString(path, StandardCharsets.UTF_8)));
      }
    } catch (IOException e) {
      throw new IndexException(IndexException.GENERAL, e.getMessage(), IR_DIR, e);
    }
    return files;
  }

  private String read(String path) throws IndexException {
    try {
      return Files.readString(projectDir.resolve(path), StandardCharsets.UTF_8);
    } catch (IOException e) {
      throw new IndexException(IndexException.GENERAL, e.getMessage(), path, e);
    }
  }

  private Map<String, String> indexedFiles() throws SQLException {
    Map<String, String> files = new TreeMap<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery("SELECT path, sha256 FROM files")) {
      while (rows.next()) {
        files.put(rows.getString(1), rows.getString(2));
      }
    }
    return files;
  }

  private Map<String, String> symbolHashes() throws SQLException {
    Map<String, String> hashes = new TreeMap<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery("SELECT id, hash FROM symbols")) {
      while (rows.next()) {
        hashes.put(rows.getString(1), rows.getString(2));
      }
    }
    return hashes;
  }

  // --- Abfragen ---

  /**
   * Änderungen je Symbol des letzten Laufs, der Dateien geändert hat.
   *
   * @return Änderungen, sortiert nach Symbol
   * @throws IndexException bei DB-Fehlern
   */
  public List<Change> changes() throws IndexException {
    List<Change> changes = new ArrayList<>();
    try (Statement statement = connection.createStatement();
        ResultSet rows =
            statement.executeQuery("SELECT symbol, change FROM changes ORDER BY symbol")) {
      while (rows.next()) {
        changes.add(new Change(rows.getString(1), rows.getString(2)));
      }
    } catch (SQLException e) {
      throw error(e);
    }
    return changes;
  }

  /**
   * Sucht Symbole, deren Name oder ID den Text enthält (ohne Groß/Klein).
   *
   * @param text Suchtext
   * @return Treffer, sortiert nach ID
   * @throws IndexException bei DB-Fehlern
   */
  public List<SymbolRow> find(String text) throws IndexException {
    String sql;
    String parameter;
    if (text.codePointCount(0, text.length()) >= 3) {
      // FTS5 mit Trigramm-Tokenizer: Teilstring ohne Groß/Klein, Text als Phrase.
      sql =
          "SELECT s.id, s.kind, s.name, s.type, s.file, s.line, s.col FROM symbols s"
              + " WHERE (s.id, s.file) IN"
              + " (SELECT f.id, f.file FROM symbols_fts f WHERE symbols_fts MATCH ?)"
              + " ORDER BY s.id, s.file";
      parameter = "\"" + text.replace("\"", "\"\"") + "\"";
    } else {
      // Trigramme brauchen mindestens drei Zeichen; kürzere Texte über LIKE.
      sql =
          "SELECT id, kind, name, type, file, line, col FROM symbols"
              + " WHERE lower(id) LIKE ? ESCAPE '\\' ORDER BY id, file";
      parameter = "%" + escapeLike(text.toLowerCase(Locale.ROOT)) + "%";
    }
    return symbols(sql, parameter);
  }

  /**
   * Sucht ein Symbol über seine ID.
   *
   * @param id Symbol-ID
   * @return Symbol oder {@code null}
   * @throws IndexException bei DB-Fehlern
   */
  public SymbolRow symbol(String id) throws IndexException {
    List<SymbolRow> rows =
        symbols(
            "SELECT id, kind, name, type, file, line, col FROM symbols WHERE id = ? ORDER BY file",
            id);
    return rows.isEmpty() ? null : rows.get(0);
  }

  /**
   * Mitglieder eines Typs in Deklarationsreihenfolge (kanonisch), ohne den Typ selbst.
   *
   * @param type qualifizierter Typname
   * @return Mitglieder
   * @throws IndexException bei DB-Fehlern
   */
  public List<SymbolRow> outline(String type) throws IndexException {
    return symbols(
        "SELECT id, kind, name, type, file, line, col FROM symbols WHERE type = ? AND id <> type"
            + " ORDER BY file, line, col",
        type);
  }

  /**
   * Alle Verwendungen eines Symbols.
   *
   * @param target Symbol-ID (Typ oder Member)
   * @return Verwendungen, sortiert nach Quelle und Ort
   * @throws IndexException bei DB-Fehlern
   */
  public List<RefRow> refs(String target) throws IndexException {
    return refs("WHERE r.target = ?", "r.source, r.file, r.line, r.col", target);
  }

  /**
   * Aufrufer einer Methode.
   *
   * @param method Symbol-ID der Methode
   * @return je aufrufendem Symbol die erste Stelle, sortiert nach Quelle
   * @throws IndexException bei DB-Fehlern
   */
  public List<RefRow> callers(String method) throws IndexException {
    return firstPerKey(
        refs("WHERE r.kind = 'call' AND r.target = ?", "r.source, r.file, r.line, r.col", method),
        true);
  }

  /**
   * Von einer Methode aufgerufene Methoden.
   *
   * @param method Symbol-ID der Methode
   * @return je Ziel die erste Stelle, sortiert nach Ziel
   * @throws IndexException bei DB-Fehlern
   */
  public List<RefRow> callees(String method) throws IndexException {
    return firstPerKey(
        refs("WHERE r.kind = 'call' AND r.source = ?", "r.target, r.file, r.line, r.col", method),
        false);
  }

  /**
   * Verwendungen einer externen API (nicht im Projekt deklariert): das Symbol selbst oder, wenn es
   * ein Typ ist, alle seine Member ({@code Typ::...}).
   *
   * @param api Symbol-ID oder Typname, z. B. {@code UnityEngine.Input}
   * @return Verwendungen, sortiert nach Ziel, Quelle und Ort
   * @throws IndexException bei DB-Fehlern
   */
  public List<RefRow> uses(String api) throws IndexException {
    String like = escapeLike(api) + "::%";
    String where =
        "WHERE (r.target = ? OR r.target LIKE ? ESCAPE '\\')"
            + " AND NOT EXISTS (SELECT 1 FROM symbols s WHERE s.id = r.target)";
    List<RefRow> rows = new ArrayList<>();
    String sql = refsSql(where, "r.target, r.source, r.file, r.line, r.col");
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, api);
      statement.setString(2, like);
      readRefs(statement, rows);
    } catch (SQLException e) {
      throw error(e);
    }
    return rows;
  }

  /**
   * Quelltext der Deklaration eines Symbols.
   *
   * @param symbol Symbol aus dem Index
   * @return S-Expression des Symbols
   * @throws IndexException Exit 1, wenn die Datei nicht lesbar ist
   */
  public String irText(SymbolRow symbol) throws IndexException {
    String location = symbol.location();
    int columnStart = location.lastIndexOf(':');
    int lineStart = location.lastIndexOf(':', columnStart - 1);
    String file = location.substring(0, lineStart);
    int line = Integer.parseInt(location.substring(lineStart + 1, columnStart));
    int column = Integer.parseInt(location.substring(columnStart + 1));
    return SymbolSource.slice(read(file), line, column);
  }

  private static String escapeLike(String text) {
    return text.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
  }

  private List<SymbolRow> symbols(String sql, String parameter) throws IndexException {
    List<SymbolRow> rows = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(sql)) {
      statement.setString(1, parameter);
      try (ResultSet result = statement.executeQuery()) {
        while (result.next()) {
          rows.add(
              new SymbolRow(
                  result.getString(1),
                  result.getString(2),
                  result.getString(3),
                  result.getString(4),
                  result.getString(5) + ":" + result.getInt(6) + ":" + result.getInt(7)));
        }
      }
    } catch (SQLException e) {
      throw error(e);
    }
    return rows;
  }

  private static String refsSql(String where, String order) {
    return "SELECT r.source, r.target, r.kind, r.file, r.line, r.col,"
        + " NOT EXISTS (SELECT 1 FROM symbols s WHERE s.id = r.target) FROM refs r "
        + where
        + " ORDER BY "
        + order;
  }

  private List<RefRow> refs(String where, String order, String parameter) throws IndexException {
    List<RefRow> rows = new ArrayList<>();
    try (PreparedStatement statement = connection.prepareStatement(refsSql(where, order))) {
      statement.setString(1, parameter);
      readRefs(statement, rows);
    } catch (SQLException e) {
      throw error(e);
    }
    return rows;
  }

  private static void readRefs(PreparedStatement statement, List<RefRow> rows) throws SQLException {
    try (ResultSet result = statement.executeQuery()) {
      while (result.next()) {
        rows.add(
            new RefRow(
                result.getString(1),
                result.getString(2),
                result.getString(3),
                result.getString(4) + ":" + result.getInt(5) + ":" + result.getInt(6),
                result.getBoolean(7)));
      }
    }
  }

  /** Erste Zeile je Quelle ({@code bySource}) bzw. je Ziel; die Eingabe ist danach sortiert. */
  private static List<RefRow> firstPerKey(List<RefRow> rows, boolean bySource) {
    List<RefRow> result = new ArrayList<>();
    String previous = null;
    for (RefRow row : rows) {
      String key = bySource ? row.source() : row.target();
      if (!key.equals(previous)) {
        result.add(row);
        previous = key;
      }
    }
    return result;
  }

  private int count(String sql) throws SQLException {
    try (Statement statement = connection.createStatement();
        ResultSet rows = statement.executeQuery(sql)) {
      return rows.next() ? rows.getInt(1) : 0;
    }
  }

  private static IndexException error(SQLException e) {
    return new IndexException(IndexException.GENERAL, "Indexfehler: " + e.getMessage(), FILE, e);
  }
}
