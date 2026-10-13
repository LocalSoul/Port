# PROGRESS.md

Wird von Agents am Ende jeder Session aktualisiert. Kurz und konkret halten.

## Aktueller Stand

- Phase: M0 (IR), M0.1 erledigt
- Aktive Aufgabe: `tasks/M0-ir.md`, nächste Teilaufgabe M0.2 (Lexer und S-Expression-Reader), Branch `task/M0-skeleton`
- Letzte Session: 2026-10-13, Setup und M0.1 (Repo-Skelett)

## Erledigt

- Setup: Git-Repo (`main`), Agent-Kit committet, `dev.example` → `io.github.localsoul`, `.gitignore` (inkl. `original/`). Ordner `original/` (Papers, Please) auf Wunsch des Nutzers gelöscht.
- M0.1 Repo-Skelett: Parent-POM `io.github.localsoul:recomp-parent` (Java 25, UTF-8, alle Plugins gepinnt, `project.build.outputTimestamp` fest), Modul `ir`, Maven-Wrapper 3.3.4 (only-script, Maven 3.9.16), Spotless (google-java-format 1.36.1, `check` in `verify`), Enforcer (Java ≥ 25, Maven ≥ 3.9, gepinnte Plugins, Dependency-Konvergenz, Verbot von Lombok/JUnit 4/Logging-Bibliotheken; in `ir` zusätzlich keine Projektmodule), JUnit 5.14.4, ArchUnit 1.5.0 (Test-Scope), Platzhaltertest.

## Offene Punkte / Nebenfunde

- .NET SDK defekt/unvollständig: `dotnet --version` meldet `/usr/lib64/dotnet/host/fxr does not contain any version-numbered child folders`. Vor M2 beheben.
- Zielspiel wird ein selbst kompiliertes 2048 (Unity, möglichst Mono-Build, siehe ADR-003). Noch nicht vorhanden.
- Statische Prüfung (Error Prone oder SpotBugs, `docs/conventions.md`) ist noch nicht im Build. Nicht Teil von M0.1; Wahl offen, vor M0-Abschluss klären.
- Die SLF4J-Warnung „No SLF4J providers were found“ beim Build stammt von Werkzeugen im Build und ist harmlos.

## Blockiert durch offene Entscheidung

- Siehe `docs/decisions.md`, Einträge mit Status `OPEN`. Aufgaben, die davon abhängen, sind in der Aufgabendatei markiert.

## Hinweise für die nächste Session

- Vor dem Start die Checkliste in `docs/setup.md` prüfen.
- Layout und Projektarten: `docs/projects.md` lesen (Spiele in `games/`, Engines in `engines/`, ADR-020 PROPOSED).
- Mit `tasks/M0-ir.md` beginnen. Repo-Skelett (Maven-Multi-Module, Wrapper, Spotless, Enforcer) ist Teil von M0.
