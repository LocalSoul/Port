# Architektur und Übersicht

Dies ist das zentrale Übersichtsdokument. Details stehen in den Modul-Dokumenten (`docs/modules/`), Entscheidungen im ADR-Log (`docs/decisions.md`).

## 1. Ziel

Eine eigene, **separate Game-Engine** plus eine Toolchain, die Spiele anderer Engines darauf lauffähig macht.

- Die Engine (`engine-core`) kennt weder Unity noch Godot. Sie hat ein eigenes, neutrales Modell.
- **Adapter** setzen oben auf: Sie bilden API und Semantik einer fremden Engine nach und sprechen mit der Engine-API. Erster Adapter: Unity, später Godot und weitere.
- Die **Spiellogik** wird halbautomatisch übersetzt: Quellcode des Spiels → eigene **IR** → Java. Der generierte Code spricht nur mit dem gewählten Adapter und `runtime-system`.
- Ein zentrales, headless **CLI-Tool** (`port`, Mischung aus Maven und Git) verwaltet Projekte, Pipeline, Index und Lint und ist primär für AI-Agents gebaut.
- **Zwei getrennte Recomp-Aufgaben** (ADR-020): *Game-Recomp* (Spiellogik eines Spiels portieren) und *Engine-Recomp* (fremde Engine als Adapter und Provider nachbilden). Beide liegen als Projekte im selben Monorepo (`games/<name>/`, `engines/<name>/`), siehe `docs/projects.md`.
- Ein **Launcher** kombiniert Spiel, Engine und Subsysteme über Profile und startet das Ergebnis.
- Langfristig lassen sich **Subsysteme** (Rendering, Input, Physik, Audio) unabhängig voneinander mischen.

## 2. Nicht-Ziele

- Keine vollautomatische Übersetzung. Etwa 80 % laufen automatisch, der Rest ist bewusste Handarbeit (`@manual`).
- Kein Nachbau von Editor, Asset-Pipeline oder Renderer-Interna fremder Engines.
- Kein generischer Übersetzer für beliebigen Code. Er deckt nur die Muster ab, die Spiele tatsächlich nutzen.
- Keine pixelgenaue oder bitgenaue Verhaltensgleichheit (Physik und Zeitschritte weichen zwischen Engines immer ab).
- Kein IL2CPP vor M10b (ADR-003, danach über das native Frontend, ADR-023).

## 3. Laufzeit-Schichten

```
Unity-Spiel        Godot-Spiel         eigenes Spiel
(generiertes Java) (generiertes Java)  (direkt gegen Engine-API)
      |                  |                   |
Unity-Adapter      Godot-Adapter             |
(+ runtime-system) (+ runtime-system)        |
      \                  |                  /
            Engine-API (stabil, versioniert)
                         |
                 Engine-Core (eigene Engine)
                         |
        Szene | Scheduler | Physik | Rendering | Audio | Input
```

Abhängigkeitsrichtung: Adapter → Engine-API ← Engine-Core. Nie umgekehrt.

## 4. Entwicklungszeit-Pipeline

```
Quellspiel -> [extract] -> [frontend] -> [ir] -> [fix, optional KI] -> [index] -> [lint] -> [gen] -> [build] -> [package/run]
```

- **extract:** Code und Assets einlesen, Hashes prüfen.
- **frontend:** Quellcode → AST → IR. Unity-C# zuerst, später GDScript. Nativer Code wird, wo immer möglich, disassembliert und gehoben (`frontend-native`, ADR-023).
- **ir:** Textdatei (S-Expressions), committed in Git, diffbar.
- **fix (optional, ADR-017):** KI-Agent bearbeitet offene Stellen (`raw`, `:manual`, Lint-Fehler). Ergebnisse liegen als geprüfter Overlay in `fixes/`, adressiert über Symbol-IDs. Aus generierter IR plus angewendeten Fixes entsteht die **effektive IR**, auf der alle folgenden Phasen arbeiten. Siehe `docs/ai-fix-phase.md`.
- **index:** SQLite (`.port/index.db`), aus der effektiven IR gebaut.
- **lint:** IR-Validator (IR/PT/SM/SH/MN/FX/NT-Regeln).
- **gen:** IR → Java, `FooBase.java`, nicht in Git.
- **build/run:** Java + Adapter + `runtime-system` + Engine + `manual/` kompilieren und starten.

Handarbeit greift an vier Stellen ein: in der IR (`:manual`), in `manual/` (Overrides), im Adapter (fehlende APIs) und im Engine-Core (fehlende Fähigkeiten).

## 5. Module und Abhängigkeiten (verbindlich)

| Modul | Aufgabe | Darf abhängen von |
|---|---|---|
| `ir` | Datenmodell, Parser, Printer, Validator der IR | nichts im Projekt |
| `engine-api` | Schnittstelle der Engine (Interfaces, Wertetypen, Komponentenschemas) | nichts im Projekt |
| `engine-core` | Implementierung der Engine, neutral | `engine-api` |
| `runtime-system` | Java-Nachbau des benötigten `System.*`-Ausschnitts | nichts im Projekt |
| `adapter-unity` | Unity-API-Nachbau (liegt in `engines/unity/`) | `engine-api`, `runtime-system` |
| `adapter-godot` | später: Godot-API-Nachbau (liegt in `engines/godot/`) | `engine-api`, `runtime-system` |
| `frontend-unity` | C# → IR (.NET-Prozess plus Java-Wrapper) | `ir` |
| `frontend-native` | ab M10b: nativer Code → IR (externer Prozess plus Java-Wrapper) | `ir` |
| `frontend-godot` | später: GDScript/C# → IR | `ir` |
| `generator-java` | IR → Java | `ir` |
| `port-cli` | zentrales Werkzeug | `ir`, `generator-java`, `frontend-*`, `launcher-core`; Adapter nur über Metadaten-Dateien |
| `launcher-core` | Katalog, Profile, Kompatibilitätsprüfung, Start | `engine-api` (nur Metadatentypen) |
| `launcher-ui` | ab M12a: Oberfläche | `launcher-core` |
| `integration-tests` | End-to-End-Tests | alles (nur Test-Scope) |
| `engines/*/providers/*` | ab M12: austauschbare Subsystem-Provider | `engine-api` |

Verboten: `engine-core` → `adapter-*`, `adapter-*` → `engine-core` (außer Test-Scope), `generator-java` → `engine-*`/`adapter-*`, Subsysteme untereinander. Generierter Spielcode importiert nur den gewählten Adapter und `runtime-system` (und deren API-Typen aus `engine-api`). Geprüft per Maven Enforcer und ArchUnit.

## 6. Rollen und Prinzipien

| Teil | Aufgabe |
|---|---|
| Engine-Core | Neutrales Modell (Entities, Scheduler-Phasen, Transform, Subsysteme). Kennt keine fremde Engine. |
| Engine-API | Schmale, versionierte Schnittstelle zum Core. |
| Adapter | API nachbilden, Semantik umsetzen (Lifecycle, Coroutines, Koordinaten), Assets laden, Abdeckungs-Metadaten liefern. |
| `runtime-system` | Java-Entsprechung der benutzten .NET-Basisbibliothek. |
| Frontend | Quellcode und Assets in IR und neutrale Daten umwandeln. |
| IR | Sprachunabhängiges, aufgelöstes, entzuckertes Zwischenformat. |
| Generator | IR → Java, ohne Wissen über die konkrete Engine. |
| CLI | Orchestriert Pipeline, Index, Lint, Git, Abhängigkeiten, Agent-Arbeitspakete. |

Prinzipien:
- **Aufgelöst:** In der IR ist jeder Typ und jede Member-Referenz voll qualifiziert.
- **Entzuckert:** Kein Syntax-Zucker in der IR.
- **Semantik bleibt erhalten:** `struct`-Wertsemantik, `checked`, `yield`.
- **Verlustfrei im Fehlerfall:** Nicht Übersetzbares steht als `(raw cs "...")` in der IR, nie still verworfen.
- **Handarbeit sauber getrennt:** Generiertes (`FooBase.java`) und Handgeschriebenes (`manual/Foo.java`) liegen getrennt, ein erneuter Lauf überschreibt keine Handarbeit.
- **Determinismus** überall, damit Git-Diffs stabil sind.
- **Adapter-Metadaten:** Jeder Adapter und `runtime-system` liefern eine maschinenlesbare Abdeckungsliste (`mapping.tsv`). Lint und `deps check` basieren darauf.

## 7. Engine-Core

Anforderungen:
- **Neutrales Modell** (ECS, ADR-016). Weder GameObject- noch Node-Baum-Modell im Core.
- **Scheduler mit Phasen** (`EARLY`, `FIXED`, `UPDATE`, `LATE`). Adapter bilden darauf ihre Reihenfolge ab. Lifecycle-Logik (`Awake`, `Start`, ...) liegt im Adapter.
- **Feste Koordinatenkonvention** (ADR-007), Konvertierung an der Adaptergrenze.
- **Austauschbare Subsysteme** mit Parametern, die Adapter setzen können.
- **Headless-Modus** mit deterministischem Fixed-Timestep, damit Tests ohne Fenster laufen.
- **Erweiterungspunkte** (Komponententypen, Events), damit Adapter Zusatzverhalten selbst bauen können.

### ECS-Entwurf (ADR-016, PROPOSED)
- **Entity:** 64-Bit-ID (Index plus Generation).
- **Komponenten:** Datenrecords mit versioniertem Schema, Sparse-Set-Speicher hinter `ComponentStore` (später austauschbar).
- **Systeme:** die Subsystem-Provider, laufend in Scheduler-Phasen, mit deklarierten Lese-/Schreibzugriffen.
- **Hierarchie:** `Parent` und geordnete `Children`, ein Transform-System berechnet `WorldTransform`.
- **Strukturänderungen** über Command-Buffer am Phasenende. Abfragen als explizite, deterministisch geordnete Schleifen.
- **Der Core kennt keine Skripte.** Objektzustände fremder Engines (z. B. `MonoBehaviour`-Instanzen) leben im Adapter und hängen über einen adapterdefinierten Komponententyp an der Entity.
- **Debugbarkeit:** `World.dump()` und `EntityView`.

## 8. Subsystem-Provider und Mischbarkeit (ADR-015, PROPOSED)

Zwei unabhängige Achsen:
- **Adapter** (welche API das Spiel spricht): Unity, Godot. Bestimmt die API-Semantik (z. B. Unity-`GetAxis` mit Glättung).
- **Provider** (welches Verhalten dahinter steckt): pro Subsystem auswählbar, z. B. `render-unity-like`, `input-godot-like`.

```
Spiel -> Adapter -> Engine-API -> Provider je Subsystem
                      |
        World (Komponenten) + Event-Bus + Scheduler-Phasen
```

Datenaustausch zwischen Subsystemen: gemeinsamer Zustand (Komponenten mit neutralen, versionierten Schemas), Events/Commands über einen Event-Bus mit deterministischer Reihenfolge, feste Synchronisationspunkte über Phasen.

Regeln:
- Subsysteme importieren sich **nie gegenseitig** und rufen sich nie direkt auf.
- Pro Komponente gibt es genau einen schreibenden Besitzer.
- Jeder Provider liefert `SubsystemInfo` (liest/schreibt Komponenten, sendet/empfängt Events, Pflichtpaare, Konflikte).
- Auswahl in `port.toml` unter `[engine.subsystems]`, Auflösung per `ServiceLoader`, Prüfung mit `port engine check`.
- Unterschiede zuerst als **Profile** (Parameter) lösen, eigene Provider nur bei Bedarf.

Grenzen: „Unity-Rendering" heißt Verhalten nachbilden, nicht Unitys Renderer verwenden. Leicht mischbar sind Input, Audio, Zeit. Schwierig sind Rendering, Szene und Physik (gemeinsames Szenenmodell).

## 9. Agent-first Tool-Design (ADR-018, PROPOSED)

Das CLI `port` ist primär für AI-Agents gebaut: JSON bei nicht-interaktiver Ausgabe, `schema`/`explain`, Arbeitspakete (`task next`), schnelle Verifikation (`verify --scope`), transaktionale Patches über Symbol-IDs, ein kleiner MCP-Server, Leitplanken (nur `fixes/`, `manual/`, `.port/` beschreibbar). Siehe `docs/agent-tooling.md`.

## 10. Projektstruktur

Das Repo ist ein Monorepo (Layout und Regeln: `docs/projects.md`). Jedes Spiel liegt als eigenes Port-Projekt in `games/<name>/`, jede Engine-Recomp in `engines/<name>/`, Zusammenstellungen in `profiles/`. Ein Spiel-Projekt sieht so aus:

```
games/mygame/
  port.toml            Manifest (wie pom.xml), kind = "game"
  port.lock            Lockfile für reproduzierbare Läufe
  input/               Eingabedateien (nicht committen)
  ir/                  generierte IR (committen)
  fixes/               KI-Fixes als Overlay (committen)
  manual/              handgeschriebene Overrides (committen)
  data/                extrahierte Szenen/Prefabs im neutralen Format (committen)
  gen/                 generiertes Java (nicht committen)
  .port/               Cache, index.db (nicht committen)
  .gitignore
```

Manifest-Beispiel und alle Abschnitte: `docs/cli-contract.md`.

## 11. Technologie-Stack

| Bereich | Wahl |
|---|---|
| Sprache der Toolchain, Engine-API, Adapter | Java 25 |
| Build | Maven (Wrapper), Multi-Module |
| CLI | picocli, optional GraalVM Native Image |
| Dekompilierung Unity | ICSharpCode.Decompiler (ILSpy), Roslyn |
| IR-Parser | handgeschrieben (ADR-004, PROPOSED) |
| Java-Generierung | eigener Emitter oder JavaPoet |
| Index | SQLite (JDBC), FTS5 |
| Git | JGit |
| Abhängigkeiten | Maven Resolver, Lockfile |
| Java-Qualität | Spotless (google-java-format), Error Prone oder SpotBugs, Maven Enforcer, ArchUnit |
| Tests | JUnit 5, Golden Files |

Der Unity-Dekompiler basiert auf .NET und läuft als separater Prozess (ADR-005, PROPOSED). Das ist die einzige Stelle, die nicht in Java liegt.

## 12. Risiken

| Risiko | Gegenmaßnahme |
|---|---|
| Core zu schmal: Adapter können nicht alles ausdrücken | Erweiterungspunkte, Adapter bauen Zusatzverhalten selbst |
| Core zu breit: er wird faktisch zur Unity-Engine | Fremde Konzepte bleiben im Adapter, Core hat nur neutrale Konzepte |
| Verhaltensabweichungen bei Physik und Zeitschritt zwischen Engines | Referenzszenen als Tests pro Adapter, Subsysteme konfigurierbar |
| IL2CPP-Spiele und nativer Code lassen sich nicht sauber dekompilieren | Vorerst nur Mono. Danach `frontend-native` (ADR-023), Absicherung durch Konformitäts- und Differenztests, Rückfall auf Handarbeit pro Funktion |
| Struct-Wertsemantik wird in Java falsch umgesetzt | Lint SM001, einheitliche Strategie im Generator (ADR-009) |
| Semantikunterschiede in der Basisbibliothek (Collections, Strings, Rundung) | `runtime-system` mit Tests je Unterschied (ADR-019) |
| Szenen und Prefabs sind Daten, nicht Code | Neutrales Datenformat und je Adapter ein eigener Loader von Anfang an |
| Adapter wachsen unkontrolliert | `deps check` und Priorisierung nach tatsächlicher Nutzung |
| KI-Fixes sind nicht deterministisch | Overlay mit Zwischenspeicher, Prüfung vor Annahme, Review-Gate |

## 13. Glossar

| Begriff | Bedeutung |
|---|---|
| Engine-Core | Die eigene, neutrale Game-Engine |
| Engine-API | Schmale, versionierte Schnittstelle zum Core |
| Game-Recomp | Projekt, das die Spiellogik eines Spiels portiert (`games/<name>/`) |
| Engine-Recomp | Projekt, das eine fremde Engine als Adapter und Provider nachbildet (`engines/<name>/`) |
| Konformitätstest | Vergleich einer Engine-Recomp mit aufgezeichnetem Originalverhalten (Szenario, Trace, Toleranz) |
| Profil | Zusammenstellung aus Spiel, Engine und Subsystem-Providern (`profiles/*.toml`) |
| Launcher | Katalog, Profilprüfung und Start (`launcher-core`, später Oberfläche) |
| Adapter | Nachbildung der API/Semantik einer fremden Engine auf der Engine-API |
| Provider | Austauschbare Implementierung eines Subsystems (Rendering, Input, ...) |
| `runtime-system` | Java-Nachbau des benötigten .NET-Basisbibliothek-Ausschnitts |
| Frontend | Wandelt Quellspiel-Code und Assets in IR und neutrale Daten um |
| IR | Intermediate Representation, eigenes Zwischenformat |
| Effektive IR | Generierte IR plus angewendete, akzeptierte KI-Fixes |
| Mono / IL2CPP | Zwei Unity-Skript-Backends: .NET-IL bzw. nach C++ übersetzt (Mono jetzt, IL2CPP ab M10b über das native Frontend) |
| `@manual` / `:manual` | Markierung für Stellen, die von Hand übersetzt werden |
| Stabile Symbol-ID | `Namespace.Typ::Member#ParamTypen` |
| 3-Wege-Merge | Alte IR, neue IR und eigene Änderungen zusammenführen |
| Lockfile | Festgeschriebene Versionen aller Abhängigkeiten |
