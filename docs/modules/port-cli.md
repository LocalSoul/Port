# Modul `port-cli`

## Zweck
Das zentrale, headless Kommandozeilenwerkzeug `port`. Orchestriert Pipeline, Index, Lint, Git und Abhängigkeiten. Vertrag: `docs/cli-contract.md`.

## Abhängigkeiten
`ir`, `generator-java`, `frontend-*`, `launcher-core` (ab M4a). Adapter, Engine und `runtime-system` nur über Dateien (Mapping, kompilierte Klassen als Klassenpfad für `build`), nie über Code (Maven Enforcer, ArchUnit). Bibliotheken: picocli, tomlj.

## Technik
- picocli für Befehle und Flags. Optional später GraalVM Native Image (M10).
- Git über JGit. TOML-Parsing über eine kleine, etablierte Bibliothek (ADR bei Hauptabhängigkeit).
- Maven-Auflösung über den Maven Resolver ab M4 oder später, vorher Pfade aus `port.toml`.
- Index (SQLite, JDBC) ab M5.

## Stand M4
- picocli über das **programmatische Modell** (`CommandSpec`/`OptionSpec`, keine Annotationen, keine Reflection im eigenen Code). Globale Flags sind vererbt und stehen vor oder nach dem Befehl. `Main.run(args, out, err, workingDir)` liefert den Exit-Code (Tests), `Main.main` beendet die JVM.
- TOML lesen mit tomlj (Nutzerentscheidung M4); `port.toml` schreibt `init` als festen Text. JSON über ein eigenes Modell (`json/`) und einen deterministischen Writer.
- `PlatformLocator` findet Frontend-Programm (Konfiguration, sonst gebaute DLL der Plattform, sonst `PATH`), Mappings und Klassenpfad (`target/classes` von Adapter, `runtime-system`, `engine-api`, `engine-core`). Er wird in M4a durch Katalog und Profile ersetzt. Die Plattform muss gebaut sein (`./mvnw -q install`, `scripts/build-dotnet.sh`).
- Kein Git (JGit bräuchte slf4j-api, das der Enforcer verbietet; Entscheidung mit ADR, wenn die Git-Befehle drankommen).
- `status`: `manualOpen` zählt `raw`-/`manual`-Knoten bzw. Knoten mit `:manual` in `ir/` (über den S-Expression-Leser), Typen mit Klasse in `manual/` gelten als erledigt.
- Start von Hand: Klassenpfad aus `port-cli`, `ir`, `generator-java`, `frontend-unity`, picocli, tomlj, antlr4-runtime; ein ausführbares Paket kommt später (M10 Native Image).

## Aufbau (Pakete)
```
cli/        Befehlsklassen (dünn: Argumente lesen, Dienst aufrufen, Ergebnis ausgeben), Main
core/       Dienste: Projekt, Manifest, Pipeline mit Phasen (extract, ir, gen, build), Status, Exit-Codes
json/       unveränderliches JSON-Modell und deterministischer Writer
output/     Ausgabe: Text-Renderer und JSON-Renderer (Hülle laut Vertrag)
agent/      Arbeitspakete, Verifikation, Patches, MCP-Server (ab M6a, siehe docs/agent-tooling.md)
catalog/     Projektkatalog und Launcher-Befehle (dünn, Logik in launcher-core, ab M4a)
fix/        KI-Fix-Phase: Overlay, Zwischenspeicher, Review (ab M7a, siehe docs/ai-fix-phase.md)
```
Befehle enthalten keine Fachlogik. Fachlogik liegt in `core/` und ist ohne CLI testbar.

## Regeln
- Jeder Befehl liefert ein Ergebnisobjekt, der Renderer macht daraus Text oder JSON.
- Exit-Code wird zentral aus dem Ergebnis abgeleitet (Tabelle im CLI-Vertrag).
- Alle Dateischreibvorgänge atomar und idempotent.
- Keine interaktiven Abfragen.

## Tests
- Befehle gegen temporäre Projektordner (`@TempDir`) mit synthetischen Fixtures.
- Golden Files für `--json`-Ausgaben (`testdata/golden/cli/`, Aktualisieren mit `-Dgolden.update=true`).
- End-to-End auf dem Testspiel (`PipelineEndToEndTest`); ohne gebautes .NET-Frontend werden diese Tests mit Begründung übersprungen.
- Exit-Code-Tests für alle Fehlerfälle des Vertrags.

## Nicht-Ziele
- Keine Engine- oder Adapter-Logik.
- Keine GUI (die Oberfläche kommt als `launcher-ui`, ADR-021).
