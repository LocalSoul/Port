# M4a: Monorepo-Layout, Katalog und Launcher (headless)

**Ziel:** Mehrere Spiele und mehrere Engines liegen als getrennte Projekte im Repo. Ein Profil kombiniert Spiel und Engine, `port launch run` startet das Ergebnis.
**Eingaben:** `AGENTS.md`, `docs/projects.md`, `docs/modules/launcher.md`, `docs/cli-contract.md`, `docs/decisions.md` (ADR-015, 020).
**Voraussetzung:** M3 und M4 fertig (Testspiel läuft über `port run`).

## M4a.1 Layout und Manifeste
1. `engines/unity/` als Engine-Recomp-Projekt mit `port.toml` (`kind = "engine"`, `[engine.provides]`). `adapter-unity` liegt dort (falls noch nicht in M3 so angelegt: verschieben, Maven-Modulname bleibt).
2. Testspiel nach `games/<name>/` überführen (`kind = "game"`, `[game.requires]`).
3. Manifest-Parser in `port-cli` für beide Arten, mit Validierung (unbekannte `kind`, fehlende Pflichtfelder → Exit 2).
4. `port init --kind engine|game`.

**Abnahme:**
- [x] Beide Manifestarten werden gelesen und validiert (Tests für gültig und jeden Fehlerfall).
- [x] `port init --kind engine` und `--kind game` erzeugen das richtige Gerüst (Golden-Test).
- [x] Das Spiel baut weiterhin mit `port build`, der Root-Build (`./mvnw -q verify`) enthält die Engine-Module, aber kein Spiel.

## M4a.2 Katalog
1. Modul `launcher/launcher-core`: durchsucht `games/*`, `engines/*` nach `port.toml`, liest Provider aus `engines/*/providers/*`.
2. `port catalog list` und `port catalog show`.

**Abnahme:**
- [ ] Katalog gegen ein synthetisches Mini-Repo mit einem Spiel und zwei Engines (`@TempDir`), Ausgabe deterministisch sortiert.
- [ ] `--json` mit Golden File, Paginierung funktioniert.
- [ ] Ein defektes Unterprojekt erscheint als Diagnose, bricht den Katalog nicht ab.

## M4a.3 Profile und Prüfung
1. Profilformat laut `docs/projects.md`, Lesen und Schreiben (`port launch new`, `list`).
2. `port launch check`: Adapter-Name, API-Version (Bereichsprüfung), Core-API-Version. Provider-Prüfung ab M12, vorher gelten die Standard-Provider des Adapters.

**Abnahme:**
- [ ] Testfälle: passend, falscher Adapter, API-Version außerhalb des Bereichs, fehlendes Spiel, fehlende Engine.
- [ ] Fehler enthalten einen vorgeschlagenen nächsten Befehl (ADR-018).

## M4a.4 Start
1. `port launch run`: bei Bedarf bauen (Hash der Eingaben), Klassenpfad aus Artefakten zusammensetzen, neue JVM starten, Exit-Code durchreichen.

**Abnahme:**
- [ ] Das Testspiel startet über ein Profil headless und liefert die erwartete Ausgabe (Integrationstest).
- [ ] Veraltetes Artefakt löst einen Bau aus, aktuelles nicht (Test).
- [ ] Der Launcher selbst hat kein Spiel und keinen Adapter im Klassenpfad (ArchUnit).

## Nicht-Ziele
Oberfläche (M12a), Provider-Mischung (M12), Download von Spielen, Git-Submodule.

## Abhängigkeiten/Entscheidungen
ADR-020 (PROPOSED, folgen und im Bericht nennen), ADR-015 (nur Standard-Provider), ADR-021 (OPEN, nicht berühren), ADR-010 (OPEN, nicht berühren).

Vom Nutzer entschieden (2026-10-13):
- **Startpunkt:** Startliste im Profil (`[run] spawn`, `frames`, `args`); ein headless Starter im Adapter legt je Eintrag ein GameObject mit dem Skript an, läuft N Frames und gibt den Welt-Dump aus. Übergang bis ADR-010/M8.
- **Spiele:** `games/demo` (synthetische Fixtures, startet) und `games/starcollector` (Katalog und Prüfung, startet noch nicht).
- **Bauen:** `launch run` baut das Spiel bei veraltetem Hash selbst (gen + build); Engine und Plattform werden nur geprüft (fehlend oder älter als die Quellen → Exit 8 mit Befehl). Kein Maven-Aufruf aus dem Launcher.
- **Git (2026-10-14, ADR-026):** `port init` legt für jedes Spiel und jede Engine ein eigenes Git-Repo mit erstem Commit an (Programm `git`), im Monorepo als Submodul mit relativer URL (nur gestaged). `engines/unity`, `games/demo`, `games/starcollector` werden ebenso umgestellt.
- **Mapping-Ort:** über `engines/<e>/port.toml`, `[engine.build] module`; Mapping aus `<modul>/src/main/resources/mapping.tsv`, Klassen aus `target/classes`.
