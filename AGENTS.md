# AGENTS.md

Anweisungen für AI-Coding-Agents in diesem Repository. Lies diese Datei vollständig, bevor du etwas änderst.

## Projekt in drei Sätzen

Wir bauen eine **eigene, separate Game-Engine** (`engine-core`) und eine **Toolchain** (`port`), die Spiele anderer Engines darauf lauffähig macht. **Adapter** (zuerst Unity, später Godot) bilden die API fremder Engines nach und sprechen mit der Engine-API. Die Spiellogik wird halbautomatisch übersetzt: Quellcode → eigene **IR** (S-Expressions) → **Java**.

## Lesereihenfolge bei jedem Sessionstart

1. `AGENTS.md` (diese Datei)
2. `PROGRESS.md` (aktueller Stand, offene Punkte, letzte Session)
3. Die aktuelle Aufgabendatei in `tasks/`
4. Nur bei Bedarf: `docs/architecture.md`, `docs/ir-spec.md`, `docs/modules/<modul>.md`, `docs/cli-contract.md`, `docs/decisions.md`, `docs/ai-fix-phase.md`, `docs/agent-tooling.md`, `docs/type-mapping.md`, `docs/projects.md`, `docs/setup.md`

Am Ende jeder Session: `PROGRESS.md` aktualisieren.

## Harte Regeln

1. **Abhängigkeitsrichtung ist verbindlich:**
   `adapter-*` → `engine-api` ← `engine-core`. `engine-core` importiert nie einen Adapter oder ein Frontend. `ir` hängt von keinem anderen Modul ab. Generierter Code importiert nur den gewählten Adapter und `runtime-system`, nie `engine-core`. `runtime-system` hängt von keinem anderen Projektmodul ab. Das wird per Maven Enforcer und ArchUnit-Tests geprüft. Schlägt ein Test fehl, wird die Struktur korrigiert, nicht der Test.
2. **`docs/ir-spec.md` ist die Quelle der Wahrheit für die IR.** Parser, Printer, Validator und Generator müssen ihr entsprechen. Änderungen an der Spezifikation brauchen zuerst einen Eintrag in `docs/decisions.md` und die Freigabe des Nutzers, dann ändert man die Spezifikation, dann den Code.
3. **Offene Entscheidungen nicht stillschweigend treffen.** In `docs/decisions.md` stehen Entscheidungen mit Status `OPEN` oder `PROPOSED`. Du darfst eine Option vorschlagen und hinter einem Interface implementieren, aber du entscheidest nicht allein und baust nichts, was die Entscheidung unumkehrbar macht. Im Zweifel anhalten und fragen.
4. **Determinismus:** Gleiche Eingabe erzeugt byte-identische Ausgabe (IR, Index-Dumps, JSON). Keine Zeitstempel, keine Zufallswerte, keine Abhängigkeit von `HashMap`-Iterationsreihenfolge in Ausgaben. Für Ausgaben `LinkedHashMap` oder sortierte Strukturen nutzen.
5. **Nie committen:** `input/` (Eingabedateien eines Port-Projekts), generiertes Java (`gen/`), `.port/`, Build-Ausgaben (`target/`).
6. **Stabile Symbol-IDs:** `Namespace.Typ::Member#Param1,Param2`. Einmal erzeugte Formate nicht ändern, ohne die Spezifikation und alle Konsumenten (Index, Lint, Overrides) anzupassen.
7. **Kein Scope Creep:** Arbeite nur an der aktuellen Aufgabe. Nebenfunde gehören als Eintrag unter „Offene Punkte" in `PROGRESS.md`, nicht in den aktuellen Diff.
8. **Tests sind Pflicht:** Keine Funktion ohne Test. Fehler zuerst per Test reproduzieren, dann beheben.
9. **Testdaten:** Teste mit synthetischen Fixtures, die im Repo erzeugt werden, nicht mit echten kommerziellen Spielen.
10. **Subsysteme sind entkoppelt (ADR-015):** Input, Rendering, Physik, Audio usw. importieren und rufen sich nie gegenseitig auf. Sie tauschen Daten nur über Welt-Komponenten (neutrale, versionierte Schemas) und den Event-Bus des Cores aus. Pro Komponente gibt es genau einen schreibenden Besitzer. Jeder Provider deklariert seine Verträge in `SubsystemInfo`.
11. **Generierte IR nie direkt von Hand oder per Agent ändern.** Korrekturen laufen über Overlays: KI-Fixes in `fixes/`, Handarbeit in `manual/`, jeweils über stabile Symbol-IDs (ADR-017, `docs/ai-fix-phase.md`). Agent-Befehle dürfen nur in `fixes/`, `manual/` und `.port/` schreiben.
12. **Spiele und Engines bleiben getrennte Projekte (ADR-020).** Ein Game-Projekt importiert nie eine Engine-Recomp-Implementierung, nur die API, die es braucht (`requires`). Eine Engine-Recomp enthält nie Spielcode. Die Zusammenstellung passiert nur in Profilen und im Launcher.
13. **Der Core kennt keine Skripte und keine fremden Engine-Begriffe.** Objektzustände fremder Engines (z. B. `MonoBehaviour`-Instanzen) leben im Adapter (ADR-016).

## Codestil (Kurzfassung, Details in `docs/conventions.md`)

- Java 25, Maven (Wrapper `./mvnw`), JUnit 5.
- **Klassischer, imperativer Stil.** Code muss sich mit einem Debugger Schritt für Schritt nachvollziehen lassen. Keine langen Stream-/Lambda-Ketten in Kernlogik, keine verschachtelten Optional-Konstruktionen, kein Lombok, keine Reflection-Magie.
- Kleine Methoden, sprechende Namen, explizite Fehlerbehandlung mit eigenen Exception-Typen.
- Records und sealed Interfaces sind für reine Datenmodelle (z. B. IR-Knoten) erlaubt.
- Öffentliche APIs mit Javadoc, die Verhalten und Fehlerfälle beschreibt.

## Wichtige Befehle

```
./mvnw -q verify                      # alles bauen und testen
./mvnw -q -pl ir test                 # nur ein Modul testen
./mvnw -q -pl ir -am verify           # Modul samt Abhängigkeiten
./mvnw -q spotless:apply              # Formatierung
dotnet test frontend-unity/dotnet     # .NET-Teil des Unity-Frontends
```

Vor jedem Commit muss `./mvnw -q verify` grün sein.

## Repository-Layout

```
AGENTS.md  CLAUDE.md  PROGRESS.md  README.md
docs/                 Architektur, Konventionen, Entscheidungen, Roadmap, Modulbeschreibungen
tasks/                Aufgaben mit Abnahmekriterien (M0 bis M4 detailliert)

# Plattform (gemeinsam für alle Projekte)
ir/                   IR-Grammatik, Parser, Printer, Validator
engine-api/           öffentliche Engine-Schnittstelle
engine-core/          Implementierung der Engine
runtime-system/       Java-Nachbau des benötigten System.*-Ausschnitts (BCL)
frontend-unity/       C#-Dekompilierung -> IR (.NET-Hilfsprogramm + Java-Wrapper)
frontend-native/      ab M10b: nativer Code disassemblieren und heben -> IR (ADR-023)
generator-java/       IR -> Java
port-cli/             zentrales CLI-Tool `port`
launcher/             launcher-core (Katalog, Profile, Start), später launcher-ui
integration-tests/    End-to-End-Tests (generiertes Java gegen Adapter und Engine)

# Projekte (siehe docs/projects.md)
engines/<name>/       Engine-Recomp: Adapter und Provider (z. B. engines/unity/adapter-unity)
games/<name>/         Game-Recomp: ein Port-Projekt pro Spiel (ir/, fixes/, manual/, data/)
profiles/             Profile: Spiel + Engine + Subsysteme für den Launcher

testdata/             synthetische Fixtures (Unity-Stubs, Mini-Spiele, Golden Files)
```

Game-Recomp (Spiellogik portieren) und Engine-Recomp (fremde Engine nachbilden) sind getrennte Aufgaben mit getrennten Projekten. Ein Spiel kennt nur Namen und Version der Engine-API, die es braucht, nie deren Implementierung. Jedes Spiel, das portiert wird, liegt in `games/<name>/`.

Module, die noch nicht existieren, legst du erst an, wenn die zugehörige Aufgabe sie verlangt.

## Fertig heißt

Eine Aufgabe ist erst fertig, wenn alle Punkte ihrer Abnahmekriterien erfüllt sind, `./mvnw -q verify` grün ist, relevante Dokumente aktualisiert sind und `PROGRESS.md` den neuen Stand zeigt. Was du nicht geschafft oder nicht geprüft hast, schreibst du ehrlich dazu.

## Wann du anhalten und fragen sollst

- Eine Spezifikation ist widersprüchlich oder unvollständig.
- Eine Aufgabe berührt eine offene Entscheidung.
- Du müsstest eine Regel aus diesem Dokument brechen.
- Du würdest Dateien löschen, die du nicht selbst erzeugt hast.
- Eine Aufgabe ist deutlich größer als beschrieben.
