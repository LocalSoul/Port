# M5: Index (SQLite) und Suchbefehle

**Ziel:** Ein inkrementeller Index über die IR eines Projekts (`.port/index.db`) und die Suchbefehle `find`, `refs`, `callers`, `callees`, `uses`, `show`, `outline`. Grundlage für Lint (M6) und Agent-Tooling (M6a).

**Eingaben:** `AGENTS.md`, `docs/architecture.md` (Pipeline, Technologie-Stack), `docs/cli-contract.md` (Suche und Navigation), `docs/ir-spec.md` (Symbol-IDs), `docs/agent-tooling.md` (Paginierung).

**Abhängigkeiten/Entscheidungen**
- M4a ist abgeschlossen.
- Vom Nutzer entschieden (2026-10-14):
  - Speicher: **SQLite** wie in der Architektur (xerial sqlite-jdbc 3.53.4.0, `.port/index.db`, FTS5). Der Treiber bringt slf4j-api nur `optional` mit, der Enforcer bleibt unverändert; Logging des Treibers läuft über `java.util.logging`.
  - `irChanged` in `port status`: Symbole, die sich beim letzten Indexlauf geändert haben (neu, geändert, entfernt).

---

## M5.1 Referenzen und Deklarationen als API in `ir`

**Schritte**
1. Referenzsammlung des Validators öffentlich im Paket `io.github.localsoul.port.ir.refs` (`References.collect`), der Validator nutzt sie.
2. `Declarations.collect`: alle Deklarationen mit Symbol-ID, Art, Name, Position.

**Abnahme**
- [x] Validator-Tests unverändert grün.
- [x] Tests für Referenzen und Deklarationen an den Fixtures.

## M5.2 Modul `index`

**Schritte**
1. Schema (Dateien, Symbole, Referenzen, Änderungen, FTS5), inkrementelles Update per Datei-Hash in einer Transaktion, `rebuild`.
2. Abfragen `find`, `refs`, `callers`, `callees`, `uses`, `outline`, `show`, alle deterministisch sortiert.

**Abnahme**
- [x] Zweiter Lauf ohne Änderung liest keine Datei neu; geänderte Datei wird allein neu gelesen; entfernte Datei verschwindet.
- [x] Änderungen je Symbol (neu, geändert, entfernt) stimmen.
- [x] Abfragen liefern die erwarteten Listen an den Fixtures.

## M5.3 CLI

**Schritte**
1. Phase `index` zwischen `ir` und `gen`, `port index [--rebuild]`.
2. Suchbefehle mit `--json`, Paginierung, Exit 2 bei unbekanntem Symbol mit nächstem Befehl.
3. `port status` mit `irChanged` aus dem Index.

**Abnahme**
- [x] Golden-JSON für `find` und `outline`, Paginierung, Exit-Codes.
- [x] `irChanged` zählt nach einer IR-Änderung die geänderten Symbole.

## M5.4 Abschluss
- [x] Doku `docs/modules/index.md`, `docs/cli-contract.md`, `docs/modules/port-cli.md` aktualisiert.
- [x] `PROGRESS.md` zeigt den Stand und die Lücken.

## Nicht-Ziele
Effektive IR mit Fixes (M7a), Lint (M6), `--context`/`--budget` und `port schema` (M6a), Suche im Java- oder C#-Text.
