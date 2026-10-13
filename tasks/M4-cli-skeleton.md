# M4: CLI-Grundgerüst (`port`)

**Ziel:** Ein headless CLI `port` mit `init`, `extract`, `ir`, `gen`, `build`, `status`, das die Pipeline aus M0 bis M3 auf dem synthetischen Testspiel steuert. `--json` und Exit-Codes nach `docs/cli-contract.md`.

**Eingaben:** `AGENTS.md`, `docs/cli-contract.md`, `docs/modules/port-cli.md`, `docs/architecture.md`.

**Abhängigkeiten/Entscheidungen**
- M0 bis M3 sind abgeschlossen.
- Neue Hauptabhängigkeiten (picocli, TOML-Bibliothek) vorab im Plan nennen.
- Vom Nutzer entschieden (2026-10-13):
  - TOML: **tomlj** (nur Lesen, mit Fehlerpositionen); `port.toml` schreibt `init` als festen Text.
  - JSON: **eigener kleiner Writer** (feste Schlüsselreihenfolge, keine Abhängigkeit).
  - Git: **kein Git-Init in M4** (JGit bringt slf4j-api mit, das der Enforcer verbietet; Entscheidung mit ADR, wenn die Git-Befehle drankommen).
  - Referenz-Assemblies: `init --source` kopiert die Assembly samt aller `*.dll` aus ihrem Ordner nach `input/` (bei Unity `Managed/`); `ir` warnt bei nicht aufgelösten Typen (`PORT-UNRESOLVED`, IR003).
  - Engine-Pfad bis M4a: `--adapter <pfad>` mit Monorepo-Konvention als Standard, gekapselt in `PlatformLocator` (wird in M4a durch Katalog/Profile ersetzt). Plattform außerhalb des Monorepos über `PORT_HOME`.

---

## M4.1 Gerüst und Ausgabe

**Schritte**
1. Modul `port-cli`, picocli, Pakete `cli/`, `core/`, `output/` (siehe Modulbeschreibung).
2. Globale Flags: `--json`, `-C`, `-q`, `-v`, `--offline`, `--strict`.
3. Ergebnisobjekt pro Befehl, zentrale Ableitung des Exit-Codes, Renderer für Text und JSON-Hülle.

**Abnahme**
- [x] JSON-Hülle entspricht dem Vertrag (`schema`, `command`, `ok`, `exitCode`, `data`, `diagnostics`), Golden-File-Test.
- [x] Mit `--json` steht auf stdout genau ein JSON-Objekt, sonst nichts.
- [x] Alle Exit-Codes des Vertrags sind in einem Testfall erreichbar oder als „noch nicht erreichbar" dokumentiert.

## M4.2 `init` und Manifest

**Schritte**
1. `port init <name> [--frontend unity] [--source <pfad>]`: erzeugt `port.toml` (mit `[project] kind = "game"`, `[game.requires]`), `.gitignore`, Ordner `input/`, `ir/`, `manual/`, `data/`. ~~Außerhalb eines Git-Repos wird eines angelegt (JGit)~~ (verschoben, Nutzerentscheidung: `init` meldet nur `info`), innerhalb des Monorepos ohnehin nicht. Das Zielverzeichnis ist `games/<name>/`, wenn der Befehl im Monorepo läuft (`--kind engine` kommt in M4a).
2. Manifest lesen und validieren (Ungültiges → Exit 2 mit Diagnose und Position).

**Abnahme**
- [x] `init` auf leerem Ordner erzeugt die Struktur aus dem Vertrag, wiederholter Aufruf ändert nichts (Idempotenz) oder meldet klar „existiert".
- [x] Ungültige `port.toml` führt zu Exit 2 mit verständlicher Diagnose.

## M4.3 `extract`, `ir`

**Schritte**
1. `extract`: Eingabe-Datei prüfen (Existenz, SHA-256 gegen Manifest), Ergebnis in `.port/`.
2. `ir`: Frontend aufrufen (M2), IR in `ir/` schreiben (kanonisch, atomar).

**Abnahme**
- [x] Hash-Abweichung führt zu Exit 3.
- [x] Zweiter `ir`-Lauf ohne Änderung schreibt keine Dateien neu (Zeitstempel/Inhalt unverändert).
- [x] Ohne .NET SDK: klare Fehlermeldung, Tests des Frontends werden übersprungen und benannt.

## M4.4 `gen`, `build`, `status`

**Schritte**
1. `gen`: Generator aufrufen, Ergebnis in `gen/`, `todo`-Liste in `data` und `diagnostics`.
2. `build`: Java kompilieren (`javax.tools`), Fehler → Exit 7.
3. `status`: Phase, Anzahl Klassen, offene `@manual`, geänderte IR-Knoten (soweit ohne Index möglich, sonst Zähler 0 mit Hinweis).
4. `--from`/`--only` für Lifecycle-Befehle.

**Abnahme**
- [x] Auf dem Testspiel laufen `init → extract → ir → gen → build` durch, `status` zeigt konsistente Werte.
- [x] Kompilierfehler im generierten Java erzeugt Exit 7 mit Diagnose.
- [x] `--strict` macht offene `@manual`-Stellen zu Exit 5.

## M4.5 Abschluss
- [x] `docs/cli-contract.md` und `docs/modules/port-cli.md` entsprechen dem Stand.
- [x] Abweichungen vom Vertrag (falls nötig) sind als ADR eingetragen und mit dem Nutzer abgestimmt. (Keine Abweichung; Ergänzungen wie `PORT_HOME`, `PORT-*`-IDs und `data` der Lifecycle-Befehle stehen im Vertrag. Git-Init ist nach Nutzerentscheidung verschoben, betrifft nur die Aufgabe.)

## Nicht-Ziele
Index, Suchbefehle, Lint, `ir --update`, `deps`, `adapter`-Befehle, GraalVM Native Image (kommen in M5 bis M10).
