# Modul `port-cli`

## Zweck
Das zentrale, headless Kommandozeilenwerkzeug `port`. Orchestriert Pipeline, Index, Lint, Git und Abhängigkeiten. Vertrag: `docs/cli-contract.md`.

## Abhängigkeiten
`ir`, `generator-java`, `frontend-*`, `launcher-core`. Adapter nur über deren Metadaten-Dateien, nie über Code.

## Technik
- picocli für Befehle und Flags. Optional später GraalVM Native Image (M10).
- Git über JGit. TOML-Parsing über eine kleine, etablierte Bibliothek (ADR bei Hauptabhängigkeit).
- Maven-Auflösung über den Maven Resolver ab M4 oder später, vorher Pfade aus `port.toml`.
- Index (SQLite, JDBC) ab M5.

## Aufbau (Pakete)
```
cli/        Befehlsklassen (dünn: Argumente lesen, Dienst aufrufen, Ergebnis ausgeben)
core/       Dienste: Projekt, Pipeline, Phasen, Manifest
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
- Golden Files für `--json`-Ausgaben.
- Exit-Code-Tests für alle Fehlerfälle des Vertrags.

## Nicht-Ziele
- Keine Engine- oder Adapter-Logik.
- Keine GUI (die Oberfläche kommt als `launcher-ui`, ADR-021).
