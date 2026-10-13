# CLI-Vertrag (`port`)

Verbindliche Spezifikation für Befehle, Flags, Ausgaben und Exit-Codes. Implementierung: `port-cli` (Java, picocli).

## Globale Flags

| Flag | Bedeutung |
|---|---|
| `--json` | maschinenlesbare Ausgabe (siehe Hülle unten), nichts anderes auf stdout |
| `-C <dir>` | Projektordner (Standard: aktuelles Verzeichnis) |
| `-q` / `-v` | leiser / ausführlicher |
| `--offline` | keine Netzwerkzugriffe (Abhängigkeitsauflösung nur aus Cache) |
| `--strict` | Warnungen mit Exit-Code 5 bzw. 9 behandeln, wo vorgesehen |
| `--dry-run` | schreibende Befehle zeigen nur, was sie tun würden (ADR-018) |
| `--limit <n>` / `--cursor <c>` | Paginierung bei Listenbefehlen (ADR-018) |
| `--budget <zeichen>` | begrenzt die Größe der Ausgabe bei Kontextbefehlen (ADR-018) |

Bei nicht-interaktiver Ausgabe (kein Terminal) ist JSON der Standard (ADR-018).

## Befehle

### Projekt
| Befehl | Zweck |
|---|---|
| `port init <name> [--kind game\|engine] [--frontend unity] [--source <pfad>]` | Projekt unter `games/<name>/` bzw. `engines/<name>/` mit `port.toml` und `.gitignore` anlegen (im Monorepo kein neues Git-Repo) |
| `port status` | Phase pro Assembly, geänderte IR-Knoten, offene `@manual` |
| `port todo [--class X]` | offene `@manual`-Stellen auflisten |
| `port clean [--all]` | `gen/` und Cache löschen (`--all` auch `.port/`) |

### Lifecycle (Folgephasen laufen mit, `--from`/`--only` steuern das)
| Befehl | Zweck |
|---|---|
| `port extract` | Code und Assets einlesen, Hashes prüfen |
| `port ir [--update]` | IR erzeugen, `--update` macht 3-Wege-Merge |
| `port index [--rebuild]` | Index aufbauen oder aktualisieren |
| `port lint [--rule ID] [--class X] [--fix-hint]` | IR validieren |
| `port gen [--class X]` | Java generieren |
| `port build` | Java kompilieren |
| `port package` | lauffähiges Artefakt bauen |
| `port run` | auf der eigenen Engine starten |

`build`, `package` und `run` brauchen eine Engine zum Kompilieren bzw. Starten: `--profile <name>` oder `--adapter <engines/pfad>`; ohne Angabe gilt `launch.toml` im Spielordner. `port launch run` ist der übliche Weg für den Start (siehe unten).

### Suche und Navigation (auf dem Index)
`port find <name>`, `port refs <symbol>`, `port callers <methode>`, `port callees <methode>`, `port uses <Adapter-API>`, `port show <symbol> [--ir|--src|--java]`, `port outline <klasse>`.

### Abhängigkeiten und Adapter
`port deps`, `port deps update [<artefakt>]`, `port deps check`, `port adapter list`, `port adapter info <name>`.

### Git-Wrapper
`port diff [--ir]`, `port commit -m ...` (committet nur `ir/`, `manual/`, `data/`, `port.toml`, Lockfile), `port log`, `port branch`, `port tag`.

### Agent-Unterstützung (ab M6a, ADR-018) und KI-Fix (ab M7a, ADR-017)
| Befehl | Zweck |
|---|---|
| `port schema <befehl>` | JSON-Schema von Argumenten und Ausgabe |
| `port explain <regel-id>` | Lint-Regel erklären (Beispiel, Behebung) |
| `port task next [--kind raw\|manual\|lint] [--budget n]` | ein Arbeitspaket |
| `port task list [--limit n] [--cursor c]` | offene Arbeitspakete |
| `port verify --scope <symbol\|klasse\|datei>` | Validator, Lint, Generator, Build, Tests für den Umfang |
| `port patch apply <datei> [--dry-run]` / `port patch preview <datei>` | atomare, validierte Änderungen über Symbol-IDs |
| `port fix [--review] [--auto]` | KI-Fix-Phase (opt-in), siehe `docs/ai-fix-phase.md` |
| `port fix accept <symbol>` / `port fix reject <symbol>` | Fix annehmen oder verwerfen |
| `port mcp [--allow-write]` | MCP-Server über stdio, siehe `docs/agent-tooling.md` |

Details und Formate: `docs/agent-tooling.md`, `docs/ai-fix-phase.md`.

### Katalog und Launcher (ab M4a, ADR-020)
| Befehl | Zweck |
|---|---|
| `port catalog list [--kind game\|engine\|provider]` | alle Projekte, Engines und Provider im Repo |
| `port catalog show <name>` | Details, Anforderungen, Abdeckung |
| `port launch list` | vorhandene Profile |
| `port launch new <profil> --game <g> --engine <e> [--set <art>=<provider>]` | Profil anlegen |
| `port launch check <profil>` | Kompatibilität prüfen (Adapter, API-Version, Provider, Pflichtpaare) |
| `port launch run <profil> [--build]` | bauen falls nötig, in eigener JVM starten, Exit-Code des Spiels durchreichen |

Ausgabe immer als Ergebnisobjekt (`--json`). Fehlerhafte Kombinationen liefern Diagnosen und Exit-Code 2. Details: `docs/projects.md`, `docs/modules/launcher.md`.

### Konformität (ab M11a, ADR-022)
| Befehl | Zweck |
|---|---|
| `port conformance record <szenario> --reference <ziel>` | Referenzspur gegen das Original aufzeichnen |
| `port conformance run [--scenario X] [--subsystem S]` | Szenarien gegen die Recomp spielen und mit der Referenz vergleichen |
| `port conformance diff <szenario>` | erste abweichende Stelle anzeigen |
| `port engine verify` | API-Abdeckung plus Konformitätsrate |

Abweichung außerhalb der Toleranzen liefert Exit-Code 11.

### Subsysteme (ab M12, ADR-015)
| Befehl | Zweck |
|---|---|
| `port engine list` | verfügbare Provider je Subsystem-Art |
| `port engine info <provider>` | liest/schreibt/Events, Pflichtpaare, Konflikte |
| `port engine set <art>=<provider>` | Auswahl in `port.toml` eintragen |
| `port engine check` | Kombination prüfen (ein Besitzer pro Komponente, Pflichtpaare, Konflikte) |

Eine ungültige Kombination liefert Diagnosen und Exit-Code 2.

## Exit-Codes

| Code | Bedeutung |
|---|---|
| 0 | Erfolg |
| 1 | allgemeiner Fehler |
| 2 | ungültige Argumente, Manifest oder IR |
| 3 | Eingabe nicht gefunden oder Hash stimmt nicht |
| 4 | Transpiler-Fehler (nicht unterstützte Konstrukte) |
| 5 | offene `@manual`-Stellen (nur mit `--strict`, sonst Warnung) |
| 6 | Merge-Konflikt bei `ir --update` |
| 7 | Kompilierfehler im generierten Java |
| 8 | Abhängigkeit nicht auflösbar |
| 9 | Lint-Fehler |
| 10 | Vorgeschlagener Fix oder Patch besteht die Prüfung nicht |
| 11 | Konformitätstest weicht außerhalb der Toleranzen ab (ADR-022) |

## JSON-Hülle (`--json`)

Jeder Befehl gibt genau **ein** JSON-Objekt auf stdout aus. Alles andere (Fortschritt, Logs) geht auf stderr.

```json
{
  "schema": "port/1",
  "command": "lint",
  "ok": false,
  "exitCode": 9,
  "data": { },
  "diagnostics": [
    {
      "id": "SM001",
      "severity": "warn",
      "symbol": "Mover::Update#",
      "message": "struct-Wert wird kopiert, Zielklasse ist Referenztyp",
      "location": "ir/Mover.ir:12:5",
      "hint": "als record oder mit Kopiermethode abbilden"
    }
  ]
}
```

Regeln:
- `schema` ist `port/<ganzzahl>`. Inkompatible Änderungen erhöhen die Zahl.
- `data` ist pro Befehl dokumentiert (siehe unten), Felder dürfen hinzukommen, nie wegfallen oder ihren Typ ändern.
- `diagnostics` ist immer ein Array (leer, wenn nichts zu melden ist), sortiert nach `location`, dann `id`.
- `severity` ∈ `error`, `warn`, `info`.
- Schlüssel in Objekten stehen in fester, dokumentierter Reihenfolge. Listen sind sortiert (deterministisch).

### `data` pro Befehl (Mindestumfang)

| Befehl | `data` |
|---|---|
| `status` | `{ "phase": "...", "classes": n, "manualOpen": n, "irChanged": n }` |
| `lint` | `{ "errors": n, "warnings": n, "infos": n }` |
| `todo` | `{ "items": [ { "symbol": "...", "reason": "...", "location": "..." } ] }` |
| `find` | `{ "matches": [ { "symbol": "...", "kind": "class\|method\|field\|prop", "location": "..." } ] }` |
| `deps check` | `{ "adapter": "...", "missing": [ "..." ], "stub": [ "..." ] }` |
| `gen` | `{ "files": [ "gen/Foo.java" ], "todoCount": n }` |

## Designregeln

- **Idempotenz:** Ein zweiter Lauf ändert nichts, wenn sich die Eingabe nicht geändert hat (kein Neuschreiben von Dateien, keine Hash-Änderungen).
- **Determinismus:** gleiche Eingabe, byte-identische Ausgabe.
- **Fehler gehen in `diagnostics`**, nicht nur in Exit-Codes. Der Exit-Code fasst zusammen.
- **Keine interaktiven Abfragen** (headless). Alles ist über Flags steuerbar.
- **Dateien, die der Befehl erzeugt, werden atomar geschrieben** (temporäre Datei, dann umbenennen).

## Manifest `port.toml`

Gilt für alle Projektarten. Hier das Beispiel für ein Spiel. Das Manifest einer Engine-Recomp (`kind = "engine"`, `[engine.provides]`) und das Profilformat stehen in `docs/projects.md`.

```toml
[project]
kind = "game"                  # "game" oder "engine" (ADR-020)
name = "mygame"

[game]
frontend = "unity"
source = "input/Assembly-CSharp.dll"
sha256 = "..."

[game.requires]                # was das Spiel von einer Engine braucht
adapter    = "unity"
api        = ">=2022.3"
subsystems = ["scene", "input", "render", "audio"]

[build]
java = 25

[fix]                          # ab M7a, standardmäßig aus (ADR-017)
enabled      = false
max-attempts = 3
auto-accept  = false

[engine.subsystems]            # ab M12: Standardauswahl dieses Spiels, ein Profil überschreibt sie
render  = "render-unity-like"
input   = "input-godot-like"

[lint]
disable = ["SM007"]

[lint.severity]
SM001 = "error"
PT004 = "info"
```

## Lint-Regeln (Kurzreferenz)

| ID | Prüfung | Stufe |
|---|---|---|
| IR001 | Syntaxfehler gegen die IR-Grammatik | error |
| IR002 | Hängende Referenz (Typ, Methode, Feld fehlt) | error |
| IR003 | Typ nicht aufgelöst | error |
| IR004 | Doppeltes Symbol | error |
| IR005 | Vererbungszyklus | error |
| IR006–IR010 | nativer Dialekt (ab M10b): Label/Sprungziel, Terminator, Dialektknoten außerhalb `:native`, Operandentypen, unerreichbarer Block (siehe `docs/ir-spec.md`, 12.6) | error/warn |
| PT001 | Reflection ohne `@manual` | error |
| PT002 | `unsafe`, Pointer, `stackalloc` | error |
| PT003 | `yield` in `try/finally` oder verschachtelt | error |
| PT004 | komplexes LINQ (`GroupBy`, `Join`) | warn |
| PT005 | `async`/`await`, Threads, `Task` | warn |
| PT006 | Operator-Overloading auf eigenen Typen | info |
| PT007 | Delegates/Events mit mehreren Subscribern | warn |
| SM001 | `struct` wird kopiert, Zielklasse ist Referenz | warn |
| SM002 | Unsigned-Typen | warn |
| SM003 | `checked`/`unchecked`, Überlauf | warn |
| SM004 | Ganzzahldivision/Modulo mit negativen Werten | info |
| SM005 | `float`/`double` gemischt ohne Cast | info |
| SM006 | String-Vergleich ohne `StringComparison` | warn |
| SM007 | `char` als Zahl | info |
| SM008 | Mehrdimensionale Arrays | warn |
| SM009 | Standard-Initialisierung von `struct`-Feldern | info |
| SH001 | genutzte API fehlt im Adapter | error |
| SH002 | API nur als Stub vorhanden | warn |
| SH003 | veraltete Adapter-Version laut Lockfile | info |
| MN001 | Override zeigt auf nicht mehr existierende Methode | error |
| MN002 | Signatur seit dem Override geändert | error |
| MN003 | `@manual` ohne Override | warn |
| MN004 | Override ohne `@manual`-Marker | info |
| FX001 | KI-Fix ist `stale` (Originalknoten hat sich geändert) | warn |
| FX002 | Angewendeter Fix besteht Validierung oder Build nicht mehr | error |
| FX003 | KI-Fix im Status `proposed` wartet auf Review | info |
| NT001 | Unaufgelöster indirekter Sprung oder Aufruf im gehobenen Code (ab M10b, ADR-023) | warn |
| NT002 | Zeigerarithmetik oder Speicherzugriff ohne rekonstruierten Typ | warn |
| NT003 | Inline-Assembler oder SIMD ohne Entsprechung | warn |
