# Setup und Startchecklist

## Voraussetzungen

| Werkzeug | Version | Wofür | Wann nötig |
|---|---|---|---|
| JDK | 25 | alles in Java | ab M0 |
| Maven | über den Wrapper `./mvnw` (keine eigene Installation nötig) | Build | ab M0 |
| Git | aktuell | Versionierung | ab M0 |
| .NET SDK | 8 oder neuer (LTS) | Unity-Frontend (Dekompilierung) | ab M2 |
| GraalVM | passend zu JDK 25, optional | Native Image des CLI | ab M10 |

Der Agent soll fehlende Werkzeuge **melden**, nicht umgehen. Tests, die ein Werkzeug brauchen (z. B. `dotnet`), werden ohne das Werkzeug übersprungen und im Bericht benannt.

## Repo anlegen

1. Neues Git-Repo erstellen.
2. Inhalt des Kits in die Repo-Wurzel kopieren (`AGENTS.md`, `CLAUDE.md`, `PROGRESS.md`, `README.md`, `docs/`, `tasks/`).
3. Platzhalter ersetzen:
   - Basis-Paket `dev.example.port` und Gruppen-ID `dev.example` → eigene Werte (einmal, am besten vor M0).
   - Projektname und Autor in `README.md`.
4. Commit: „chore: Agent-Kit hinzugefügt".

## Verzeichnisse für Projekte

```
games/<name>/      ein Port-Projekt pro Spiel (siehe docs/projects.md)
engines/<name>/    eine Engine-Recomp pro Engine
profiles/          Profile für den Launcher
```

Lege Spiele und Engines nur über `port init --kind ...` (ab M4a) oder gemäß `docs/projects.md` an. Jedes Projekt ist ein eigenes Git-Repo und im Monorepo ein Submodul mit relativer URL (ADR-026).

Klonen samt Projekten (Git erlaubt lokale Submodul-URLs nur ausdrücklich):

```
git -c protocol.file.allow=always clone --recurse-submodules <monorepo> Recomp
# in einem vorhandenen Klon:
git -c protocol.file.allow=always submodule update --init
```

Der Root-Build (`./mvnw -q verify`) braucht das Submodul `engines/unity` (Modul `adapter-unity`).

## Verzeichnisse für Testdaten

```
testdata/
  unity-stubs/       C#-Projekt UnityEngine.Stubs (nur Signaturen)
  unity-sources/     kleine C#-Spielklassen als Eingabe für das Frontend
  golden/
    ir/              erwartete IR-Dateien
    java/            erwartete generierte Java-Dateien
    json/            erwartete CLI-Ausgaben
  scenes/            Referenzszenen und Eingabeskripte für Engine-Tests
```

Golden Files werden nur bewusst aktualisiert (`-Dgolden.update=true`), siehe `docs/conventions.md`.

## Erste Befehle (nach M0.1)

```
./mvnw -q verify            # alles bauen und testen
./mvnw -q spotless:apply    # Formatierung
```

## Mit dem Agent arbeiten

Erster Prompt:

> Lies `AGENTS.md` und `PROGRESS.md`. Beginne mit `tasks/M0-ir.md`, Aufgabe M0.1. Arbeite auf einem Branch `task/M0-skeleton`. Melde dich, bevor du etwas tust, das einem `OPEN`-ADR widerspricht oder eine Regel aus `AGENTS.md` brechen würde.

Folgeprompts: eine Teilaufgabe pro Auftrag („Mach M0.2"), jeweils mit Verweis auf die Aufgabendatei. Nach jeder Teilaufgabe prüfen (siehe Review-Checkliste in `README.md`).

## CI (optional, später)

Ein Workflow, der `./mvnw -q verify` auf JDK 25 ausführt und den .NET-Teil mit `dotnet test` startet, sobald M2 dran ist. Die Wahl der CI-Plattform ist offen und nicht Teil der Aufgaben M0 bis M4.
