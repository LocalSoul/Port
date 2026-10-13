# Agent-first Tooling (ADR-018)

Status: PROPOSED. Das CLI `port` ist primär für AI-Agents gebaut. Menschen bekommen dieselben Ergebnisobjekte als Text. Umsetzung ab Meilenstein M6a.

## Prinzipien

1. **Maschinenlesbar zuerst:** Bei nicht-interaktiver Ausgabe ist JSON der Standard (`--json` bleibt ausdrücklich möglich). Hülle laut `docs/cli-contract.md`.
2. **Kleine, klar benannte Operationen** mit engem, idempotentem Effekt.
3. **Selbstbeschreibend:** Der Agent muss weder Dokumentation lesen noch raten müssen.
4. **Arbeitspakete statt Rohdaten:** Der Agent bekommt genau den Kontext für eine Aufgabe, in begrenzter Größe.
5. **Schnelle Verifikation** für kleine Umfänge, damit Versuch-und-Prüfung billig ist.
6. **Transaktional:** Änderungen sind atomar, validiert, rücknehmbar.
7. **Leitplanken:** Standardmäßig lesend, Schreibzugriff nur wo nötig, kein Netzwerk.
8. **Begrenzte Ausgabe:** Paginierung und Kürzungshinweise.

## Befehle (Ergänzung zum CLI-Vertrag)

| Befehl | Zweck |
|---|---|
| `port schema <befehl>` | JSON-Schema der Ausgabe und der Argumente des Befehls |
| `port explain <regel-id>` | Beschreibung einer Lint-Regel mit Beispiel für Verstoß und Behebung |
| `port task next [--kind raw\|manual\|lint] [--budget <zeichen>]` | genau ein Arbeitspaket (siehe unten) |
| `port task list [--limit n] [--cursor c]` | offene Arbeitspakete, paginiert |
| `port show <symbol> [--context n] [--budget <zeichen>]` | Symbol in gewünschter Stufe mit begrenztem Kontext |
| `port verify --scope <symbol\|klasse\|datei>` | Validator, Lint, Generator, Build und Tests nur für den Umfang |
| `port patch apply <datei> [--dry-run]` | Patch atomar anwenden, validieren, bei Fehler zurückrollen |
| `port patch preview <datei>` | Wirkung eines Patches als Diff anzeigen |
| `port fix ...` | siehe `docs/ai-fix-phase.md` |
| `port mcp` | startet einen MCP-Server über stdio |

Alle schreibenden Befehle kennen `--dry-run`. Alle Listenbefehle kennen `--limit` und `--cursor`.

## Selbstbeschreibung

- **`port schema`** gibt JSON-Schema aus, abgeleitet aus denselben Typen, die das JSON erzeugen (kein handgepflegtes Duplikat).
- **Diagnosen** enthalten zusätzlich zum Vertrag ein Feld `next` mit vorgeschlagenem Befehl:

```json
{
  "id": "SH001",
  "severity": "error",
  "symbol": "Game.Mover::Update#",
  "message": "UnityEngine.Physics::Raycast#... fehlt im Adapter",
  "hint": "API im Adapter ergänzen oder Stelle mit :manual markieren",
  "next": "port explain SH001"
}
```

- **Fehlermeldungen sind handlungsorientiert:** Was ist passiert, wo, was kann man tun. Keine Stacktraces im JSON, nur in `-v` auf stderr.

## Arbeitspaket (`port task next`)

```json
{
  "schema": "port-task/1",
  "taskId": "fix:Game.Mover::Update#",
  "kind": "raw",
  "symbol": "Game.Mover::Update#",
  "reason": "raw cs: komplexes LINQ (PT004)",
  "diagnostics": [ { "id": "PT004", "severity": "warn", "message": "..." } ],
  "sourceCs": "...",
  "irNode": "(method Update void () ...)",
  "context": {
    "irParent": "(class Mover ...)",
    "adapterCoverage": [ { "id": "UnityEngine.Transform::Translate#...", "status": "full" } ],
    "specSections": [ "7 Ausdrücke", "9 Unlösbares und Handarbeit" ]
  },
  "constraints": {
    "mayChange": [ "Game.Mover::Update#" ],
    "replacementKind": "method",
    "maxChars": 8000
  },
  "verify": "port verify --scope Game.Mover::Update#",
  "truncated": false
}
```

- Alles Fremde (Original-C#, Kommentare) ist **Daten**. Das Paket trennt es klar von Anweisungen (Feld `constraints`, `verify`).
- `--budget` begrenzt die Größe. Gekürzte Pakete setzen `truncated: true` und nennen, was fehlt.

## Verifikation (`port verify`)

- Läuft in dieser Reihenfolge und bricht beim ersten Fehler **nicht** ab, sondern sammelt: Parser, Validator, Lint für den Umfang, Generator, Kompilieren, Tests des Umfangs.
- Ausgabe ist minimal: Gesamtergebnis, Liste der Diagnosen, bei Bedarf `next`.
- Exit-Code nach Vertrag (2, 7, 9, 10 ...).
- Zielwert: wenige Sekunden für ein einzelnes Symbol. Dafür arbeitet `verify` inkrementell auf Index und Cache.

## Patches (`port patch`)

Format: S-Expression-Datei mit einer oder mehreren Operationen, adressiert über Symbol-IDs.

```lisp
(patch
  (replace "Game.Mover::Update#"
    (method Update void () :mods (private) (block ...)))
  (set-attr "Game.Mover::Start#" :manual "braucht Handarbeit: Reflection"))
```

- Operationen: `replace`, `set-attr`, `remove-attr`. Weitere nur mit ADR.
- Wirkung: Der Patch wird gegen die **effektive IR** angewendet, geschrieben wird in `fixes/` (für KI-Fixes) oder `manual/` (für Handarbeit), nie in die generierte `ir/`.
- Vor dem Schreiben validiert, atomar geschrieben, bei Fehler unverändert (Rollback). `--dry-run` zeigt nur den Diff.

## MCP-Server (`port mcp`)

Wenige, klar benannte Tools, jedes mit JSON-Schema:

| Tool | Entspricht | Wirkung |
|---|---|---|
| `task_next` | `port task next` | lesend |
| `show_symbol` | `port show` | lesend |
| `explain_rule` | `port explain` | lesend |
| `propose_fix` | `port fix propose` | schreibt `proposed`-Fix nach Prüfung |
| `verify` | `port verify` | lesend |
| `accept_fix` | `port fix accept` | schreibt (nur wenn Konfiguration es erlaubt) |
| `reject_fix` | `port fix reject` | schreibt |

- Der Server nutzt dieselben Dienste wie das CLI (`core/`), keine eigene Logik.
- Standardmäßig nur lesende Tools aktiv. Schreibende Tools brauchen `--allow-write` oder Eintrag im Manifest.
- Keine Netzwerkzugriffe, keine Shell-Ausführung.

## Leitplanken

| Bereich | Regel |
|---|---|
| Schreibzugriff | nur `fixes/`, `manual/`, `.port/` (Cache), nie `ir/`, `input/`, `gen/` per Agent-Befehl |
| Löschen | nie durch Agent-Befehle |
| Netzwerk | keines, außer der konfigurierte Fix-Anbieter in `port fix` |
| Größe | jede Antwort begrenzt, `truncated` bei Kürzung |
| Wiederholbarkeit | gleiche Eingabe, gleiche Ausgabe, kein Zeitstempel |

## Tests (für die Umsetzung)
- Schema-Tests: Ausgabe jedes Befehls validiert gegen das von `port schema` gelieferte Schema.
- Golden Files für `task next`, `verify`, `patch preview`.
- MCP: Tool-Liste, Schemas und ein Durchlauf `task_next → propose_fix → verify` mit Fake-Agent.
- Leitplanken: Schreibversuche außerhalb der erlaubten Ordner schlagen fehl.
