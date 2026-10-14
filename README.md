# Agent-Kit: Engine-unabhängige Portierungs-Toolchain

Dieses Kit enthält alles, was ein AI-Coding-Agent braucht, um das Projekt zu bauen: Regeln, Architektur, Spezifikationen, Entscheidungen und Aufgaben mit Abnahmekriterien.

**Das Projekt in drei Sätzen:** Eine eigene, separate Game-Engine (`engine-core`) und eine Toolchain (`port`), die Spiele anderer Engines darauf lauffähig machen. Adapter (zuerst Unity, später Godot) bilden die API fremder Engines nach, die Spiellogik wird halbautomatisch über eine eigene IR nach Java übersetzt. Alle Recomp-Projekte liegen in diesem Repo: Spiele in `games/`, nachgebildete Engines in `engines/`. Ein Launcher kombiniert Spiel, Engine und Subsysteme (Rendering, Input, Physik, Audio) und startet das Ergebnis.

## Dokumentenkarte

| Datei | Inhalt | Wann lesen |
|---|---|---|
| `AGENTS.md` / `CLAUDE.md` | Regeln für Agents, Befehle, Layout | immer zuerst |
| `PROGRESS.md` | aktueller Stand, offene Punkte | jede Session |
| `tasks/` | Aufgaben M0 bis M4 mit Abnahmekriterien | pro Aufgabe |
| `docs/architecture.md` | Ziel, Architektur, Module, Stack, Risiken, Glossar | Überblick |
| `docs/ir-spec.md` | Spezifikation der IR (Quelle der Wahrheit) | M0, M2, M3 |
| `docs/decisions.md` | ADR-Log mit Status je Entscheidung | vor Aufgaben, die eine Entscheidung berühren |
| `docs/conventions.md` | Codestil, Tests, Commits | immer bei Code |
| `docs/workflow.md` | Arbeitsablauf pro Aufgabe, Berichtsformat | pro Aufgabe |
| `docs/cli-contract.md` | CLI-Befehle, Exit-Codes, JSON, Lint-Regeln | ab M4 |
| `docs/projects.md` | Projektarten (Game-/Engine-Recomp), Monorepo-Layout, Profile, Launcher | vor M0 (Layout), M4a |
| `docs/type-mapping.md` | Abbildung IR → Java, Namenskonvention | M3, M3a |
| `docs/agent-tooling.md` | Agent-first CLI, Arbeitspakete, MCP | ab M6a |
| `docs/ai-fix-phase.md` | KI-Fix-Phase nach `ir` | ab M7a |
| `docs/modules/*.md` | je Modul: Zweck, API, Tests, Nicht-Ziele | beim jeweiligen Modul |
| `docs/roadmap.md` | Meilensteine mit Abnahme | Planung |
| `docs/setup.md` | Voraussetzungen, Startchecklist, Testdaten, `port.jar` und Spiele starten | vor dem Start |

## Startchecklist

- [x] JDK 25 installiert, `./mvnw` wird vom Agent angelegt (M0.1)
- [x] Git-Repo erstellt, Kit in die Wurzel kopiert
- [x] Platzhalter `dev.example` ersetzt durch `io.github.localsoul` (Paket und Gruppen-ID)
- [ ] .NET SDK ab M2 verfügbar
- [ ] Entscheidungen mit Status `PROPOSED` für M0/M1 durchgesehen (ADR-004, 006, 007)
- [ ] Ersten Prompt aus `docs/setup.md` abgeschickt

## Review-Checkliste für Agent-Ergebnisse

1. Sind alle Abnahmekriterien der Teilaufgabe abgehakt, und gibt es zu jedem einen Test?
2. Ist `./mvnw -q verify` grün (und `dotnet test`, falls .NET berührt wurde)? Steht im Bericht, was **nicht** geprüft wurde?
3. Wurde eine `OPEN`- oder `PROPOSED`-Entscheidung berührt, und wurde sie sauber hinter einem Interface gehalten und im Bericht genannt?
4. Hält sich der Diff an die Abhängigkeitsregeln (ArchUnit grün) und an den Stil (kein Lambda-Geflecht, kein Lombok)?
5. Sind Ausgaben deterministisch (Golden Files, Roundtrip, keine Zeitstempel)?
6. Wurden Dokumente aktualisiert, die den geänderten Teil beschreiben, und `PROGRESS.md`?
7. Gibt es Scope Creep? Nebenfunde gehören in `PROGRESS.md`, nicht in den Diff.

## Entscheidungen, die vor einem Meilenstein stehen müssen

| Meilenstein | Entscheidungen |
|---|---|
| M0 | ADR-004 (PROPOSED, kann gefolgt werden) |
| M1 | ADR-006/016, 007, 015 (PROPOSED, kann gefolgt werden), ADR-014 (nur Null-Renderer) |
| M2 | ADR-005 (PROPOSED) |
| M0 | ADR-020 (PROPOSED, Layout `games/`, `engines/`) |
| M3 / M3a | ADR-008, 009 (OPEN, nicht umsetzen), ADR-019 (Teilfrage Unsigned OPEN) |
| M8 | ADR-010 (OPEN) |
| M9 | ADR-013, 014 (OPEN) |
| M12a | ADR-021 (OPEN) |
| M10b | ADR-023 (Werkzeugwahl für den Disassembler vorher entscheiden, IR-Erweiterung freigeben) |
| M11 | ADR-012 (OPEN) |
| M11a | ADR-022 (PROPOSED) |

Vollständige Liste mit Status: `docs/decisions.md`.
