# PROGRESS.md

Wird von Agents am Ende jeder Session aktualisiert. Kurz und konkret halten.

## Aktueller Stand

- Phase: Setup erledigt, noch kein Code
- Aktive Aufgabe: keine (nächste: `tasks/M0-ir.md`, M0.1)
- Letzte Session: 2026-10-13, Setup (Git-Repo, `.gitignore`, Platzhalter ersetzt)

## Erledigt

- Setup: Git-Repo (`main`), Agent-Kit committet, `dev.example` → `io.github.localsoul`, `.gitignore` (inkl. `original/`).

## Offene Punkte / Nebenfunde

- .NET SDK defekt/unvollständig: `dotnet --version` meldet `/usr/lib64/dotnet/host/fxr does not contain any version-numbered child folders`. Vor M2 beheben.
- `original/` enthält einen kommerziellen IL2CPP-Build (Papers, Please), doppelt (`original/` und `original/game/`). Wird nicht committet; geplant ist der Wechsel auf ein selbst kompiliertes 2048.

## Blockiert durch offene Entscheidung

- Siehe `docs/decisions.md`, Einträge mit Status `OPEN`. Aufgaben, die davon abhängen, sind in der Aufgabendatei markiert.

## Hinweise für die nächste Session

- Vor dem Start die Checkliste in `docs/setup.md` prüfen.
- Layout und Projektarten: `docs/projects.md` lesen (Spiele in `games/`, Engines in `engines/`, ADR-020 PROPOSED).
- Mit `tasks/M0-ir.md` beginnen. Repo-Skelett (Maven-Multi-Module, Wrapper, Spotless, Enforcer) ist Teil von M0.
