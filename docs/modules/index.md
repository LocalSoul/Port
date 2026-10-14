# Modul `index`

## Zweck
Index über die IR eines Projekts (`ir/*.ir`) in `.port/index.db` (SQLite) und die Abfragen für Suche und Navigation (`port find`, `refs`, `callers`, `callees`, `uses`, `show`, `outline`). Grundlage für Lint (M6) und Agent-Tooling (M6a). Pipeline: `ir → index → gen` (`docs/architecture.md`).

## Abhängigkeiten
Nur `ir` (Maven Enforcer, ArchUnit). Datenbankzugriff ausschließlich über `java.sql`; der Treiber xerial sqlite-jdbc liegt nur im Laufzeit-Klassenpfad (Nutzerentscheidung M5). Er bringt slf4j-api nur `optional` mit und protokolliert über `java.util.logging`. Er lädt eine native Bibliothek: Unter Java 25 die JVM mit `--enable-native-access=ALL-UNNAMED` starten, sonst gibt es eine Warnung auf stderr.

## API
```java
try (IrIndex index = IrIndex.open(projectDir, false)) {   // true = neu aufbauen
  UpdateResult result = index.update();                  // inkrementell, eine Transaktion
  index.find("mov");  index.callers("Box::Put#T");  index.uses("UnityEngine.Input");
}
```
- `update()` liest nur neue und geänderte Dateien (SHA-256 je Datei), entfernt verschwundene; ungültige IR bricht mit `IndexException` (Exit 2, Ort) ab und lässt den vorigen Stand unverändert (Rollback).
- `changes()`: Änderungen je Symbol (`added`, `changed`, `removed`) des letzten Laufs, der Dateien geändert hat. Ein Symbol gilt als geändert, wenn sich der Quelltext seiner S-Expression ändert (beim Typ: die ganze Typdeklaration). Grundlage für `irChanged` in `port status` (Nutzerentscheidung M5).
- Abfragen, alle sortiert und deterministisch:
  - `find(text)`: Name oder ID enthält den Text, ohne Groß/Klein (FTS5 mit Trigramm-Tokenizer, unter drei Zeichen `LIKE`).
  - `symbol(id)`, `outline(type)` (Mitglieder in Deklarationsreihenfolge), `irText(symbol)` (S-Expression aus der Datei).
  - `refs(id)`: alle Verwendungen (`type`, `member` für Feld/Property/Event, `call` für Methoden und Konstruktoren, erkennbar an `#` in der Symbol-ID).
  - `callers(method)`, `callees(method)`: je Gegenstelle die erste Stelle; `callees` markiert externe Ziele.
  - `uses(api)`: Verwendungen nicht im Projekt deklarierter Symbole, genau oder mit Präfix `Typ::`.

## Schema (Version 1, bei Abweichung Neuaufbau)
`meta(key, value)`, `files(path, sha256)`, `symbols(id, kind, name, type, file, line, col, hash)`, `refs(source, target, kind, file, line, col)`, `changes(symbol, change)`, `symbols_fts` (FTS5: `id`, `name`, `file`).

## Quellen der Daten
`ir.refs.Declarations` (Deklarationen mit Symbol-ID, Art, Position) und `ir.refs.References` (Referenzen des Validators, seit M5 öffentlich).

## Tests
Fixtures aus `testdata/golden/ir/unity` in `@TempDir`: erster und zweiter Lauf, geänderte und entfernte Datei, Änderungen je Symbol, Abfragen gegen erwartete Listen, Rollback bei ungültiger IR, Neuaufbau mit gleichem Ergebnis; Ausschnitt einer S-Expression; ArchUnit.

## Nicht-Ziele
Effektive IR mit Fixes (M7a), Lint (M6), Suche im Java- oder C#-Text, Abhängigkeiten zwischen Projekten.
