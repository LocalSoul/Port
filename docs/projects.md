# Projektarten, Repo-Layout und Launcher

Dieses Repo ist ein **Monorepo** für alle Recomp-Projekte. Es gibt zwei verschiedene Arten von Recomp-Aufgaben, die getrennt behandelt werden, und einen Launcher, der beides zusammensetzt und startet. Entscheidung: ADR-020 (Layout) und ADR-021 (Launcher-Oberfläche).

## 1. Zwei Projektarten

| | Game-Recomp (`kind = "game"`) | Engine-Recomp (`kind = "engine"`) |
|---|---|---|
| Aufgabe | Die **Spiellogik** eines Spiels in Java überführen | Eine **fremde Engine** (oder Teile davon) als Adapter und Provider auf der Engine-API nachbilden |
| Eingabe | Spiel-Assemblies, Szenen, Assets | API-Oberfläche der Engine, ggf. deren Quellcode oder Referenzverhalten |
| Ausgabe | IR, generiertes Java, `manual/`, extrahierte Daten | Adapter-Modul (Maven), Provider, `mapping.tsv`, Metadaten |
| Ziel | Ein Spiel läuft | Viele Spiele dieser Engine laufen |
| Lebensdauer | pro Spiel | wächst über alle Spiele hinweg (bedarfsgesteuert) |
| Fortschrittsmaß | offene `@manual`, Lint-Fehler | Abdeckung der API (`port adapter info`), Referenzszenen |
| Werkzeug | `port`-Pipeline (extract → ir → fix → gen → build) | `port` (Abdeckung, `deps check`), Maven-Build, Referenzszenen-Tests |
| Daten, die es braucht | Angaben, welche Engine-API es spricht (`requires`) | Angaben, was es bereitstellt (`provides`) |

Beide Arten sind **entkoppelt**: Ein Game-Projekt kennt nur den Namen und die Version einer API (`requires`), nie die Implementierung. Eine Engine-Recomp weiß nichts über einzelne Spiele. Das Zusammenspiel entsteht erst im Launcher durch ein **Profil**.

Umsetzung von Engine-Recomps: Zunächst wird ein Adapter wie `adapter-unity` von Hand gebaut und über Abdeckungsmetadaten gesteuert (Anforderungen kommen aus den Spielen). Ein Engine-Recomp kann aber auch aus **Binärdateien** der Engine entstehen, mit dem Ziel **funktionaler Gleichheit**. Das beschreibt Abschnitt 6 (ADR-022).

## 2. Repo-Layout (Monorepo)

```
AGENTS.md  CLAUDE.md  PROGRESS.md  README.md
docs/  tasks/

# Plattform (gemeinsam für alle Projekte)
ir/  engine-api/  engine-core/  runtime-system/
frontend-unity/  generator-java/
port-cli/  launcher/  integration-tests/

# Engine-Recomps (je ein Unterordner, je ein Projekt)
engines/
  unity/
    port.toml                 kind = "engine", [engine.provides]
    adapter-unity/            Maven-Modul
    providers/                z. B. render-unity-like, input-unity-like
  godot/                      später
    ...

# Game-Recomps (je ein Unterordner, je ein Projekt)
games/
  mygame/
    port.toml                 kind = "game", [game.requires]
    ir/  fixes/  manual/  data/        committen
    input/  gen/  .port/               nicht committen
    launch.toml               optional: Standardprofil dieses Spiels

# Zusammenstellungen
profiles/
  mygame-unity.toml           Spiel + Engine + Subsysteme, siehe unten

testdata/                     synthetische Fixtures (Unity-Stubs, Mini-Spiele, Golden Files)
```

Regeln:
- Jedes Spiel, das portiert wird, liegt in `games/<name>/` in diesem Repo. Das Verzeichnis ist ein eigenständiges Port-Projekt (siehe `docs/cli-contract.md`, `port -C games/<name> ...`).
- Jede Engine-Recomp liegt in `engines/<name>/`. Ihre Maven-Module hängen im Root-Reactor. Der Maven-Modulname bleibt `adapter-unity` (Pfad `engines/unity/adapter-unity`).
- Spiele sind **keine** Module des Root-Reactors. Sie werden mit `port build` gebaut, damit ein defektes Spiel nie den Build der Plattform bricht.
- Unterprojekte werden vom Katalog (`port catalog`) über ihr `port.toml` gefunden, nicht über feste Listen.
- Jedes Spiel und jede Engine ist ein **eigenes Git-Repo** und im Monorepo als Submodul mit relativer URL eingetragen (ADR-026). `port init` legt beides an. Klonen mit `git clone --recurse-submodules`; Änderungen zuerst im Projekt-Repo committen, dann den Submodul-Stand im Monorepo.
- Welche Dateien committet werden, regelt je Projekt `.gitignore` (siehe AGENTS.md, Regel 5).

## 3. Manifeste

Alle Projekte nutzen `port.toml`. Der Abschnitt `[project]` legt die Art fest.

### Game
```toml
[project]
kind = "game"
name = "mygame"

[game]
frontend = "unity"
source   = "input/Assembly-CSharp.dll"

[game.requires]                  # was das Spiel von einer Engine braucht
adapter    = "unity"             # Name der API
api        = ">=2022.3"          # Versionsbereich der nachgebildeten API
subsystems = ["scene", "input", "render", "audio"]   # Arten, die das Spiel nutzt (aus dem Lint ableitbar)
```

### Engine
```toml
[project]
kind = "engine"
name = "unity"

[engine.provides]
adapter    = "unity"
api        = "2022.3"
core-api   = ">=0.1 <0.2"        # benötigte Engine-API-Version
providers  = ["render-unity-like", "input-unity-like"]

[engine.sources]                 # optional, siehe Abschnitt 6
assemblies = ["input/UnityEngine.CoreModule.dll"]   # verwaltete Binärdateien
sha256     = "..."

[conformance]
scenarios  = "conformance/scenarios"
reference  = "conformance/reference"
```

Der bisherige Abschnitt `[target]` aus der Spiele-Manifest-Vorlage entfällt: Welche konkrete Engine ein Spiel startet, steht im Profil, nicht im Spiel. Für Build und Start gibt es `--profile`, `--adapter` oder das Standardprofil `launch.toml` im Spielordner.

## 4. Profile

Ein Profil legt fest, **was** zusammen gestartet wird.

```toml
# profiles/mygame-unity.toml
[profile]
name = "mygame-unity"
game = "games/mygame"

[engine]
adapter = "engines/unity"        # muss game.requires.adapter erfüllen
core    = "engine-core"          # Core-Implementierung

[engine.subsystems]              # überschreibt Standard-Provider des Adapters
render = "render-unity-like"
input  = "input-godot-like"      # Provider darf aus einer anderen Engine-Recomp stammen

[run]
headless = false
args     = []
```

Die Kompatibilität prüft `port launch check` (siehe unten): Adapter-Name und API-Version passen zu `game.requires`, jede Subsystem-Art hat genau einen Provider, die Provider-Verträge (`SubsystemInfo`) sind kompatibel (ADR-015), die Core-API-Version passt.

## 5. Launcher

Ziel: **Spiel, Engine und Subsysteme auswählen, dann starten.**

Stufen:
1. **Headless-Launcher (CLI, M4a):** Katalog, Profile, Kompatibilitätsprüfung, Start. Das ist die Grundlage und enthält die gesamte Logik.
2. **Oberfläche (M12a, ADR-021 OPEN):** Eine Oberfläche ruft nur `launcher-core` auf. Welche Technik (Desktop, Web, Terminal), ist noch nicht entschieden.

Befehle (Vertrag in `docs/cli-contract.md`):

| Befehl | Zweck |
|---|---|
| `port catalog list [--kind game\|engine\|provider]` | alle Projekte, Engines und Provider im Repo |
| `port catalog show <name>` | Details und Abdeckung |
| `port launch list` | vorhandene Profile |
| `port launch new <profil> --game <g> --engine <e> [--set <art>=<provider>]` | Profil anlegen |
| `port launch check <profil>` | Kompatibilität prüfen |
| `port launch run <profil> [--build]` | bei Bedarf bauen, Klassenpfad zusammensetzen, in eigenem Prozess starten |

Ablauf von `launch run`:
1. Profil lesen und prüfen (`check`).
2. Fehlende oder veraltete Teile bauen (Spiel: `port build`, Engine: Maven-Modul), `--build` erzwingt das.
3. Klassenpfad aus Spiel-Artefakt, Adapter, gewählten Providern, `runtime-system`, `engine-core` zusammensetzen. Der Core findet Provider per `ServiceLoader` (ADR-015).
4. Neue JVM starten, Ausgabe durchreichen, Exit-Code des Spiels zurückgeben.

Der Launcher kennt weder Spiellogik noch Engine-Interna. Er liest nur Manifeste, Metadaten und Profile.

Mehrere Spiele und mehrere Engines nebeneinander im Repo sind der Normalfall. Beispiel: Dasselbe Spiel startet mit zwei Profilen (Unity-Rendering plus Unity-Input, Unity-Rendering plus Godot-Input), ohne dass sich am Spiel etwas ändert.

## 6. Engine-Recomp aus Binärdateien und funktionale Gleichheit (ADR-022, PROPOSED)

Eine Engine-Recomp darf ihre Eingabe aus Binärdateien der Engine lesen und soll deren Verhalten funktional gleich nachbilden. Das gilt für Spiele wie für Engines (ADR-023). Zwei Dinge sind dafür nötig: eine Quelle für das Verhalten und ein Maßstab für „gleich".

### Quellen (`[engine.sources]`)

| Quelle | Beispiel | Weg | Stand |
|---|---|---|---|
| Verwaltete Assemblies (IL) | `UnityEngine.CoreModule.dll` | dasselbe Frontend wie bei Spielen (ILSpy/Roslyn) → IR → Generator, nur mit Ziel „Engine-Modul" statt „Spiel" | M11a |
| Nur API-Oberfläche | Signaturen ohne Rumpf | Stubs, Rumpf von Hand (wie bisher) | M3 |
| Nativer Code (C/C++-Binärdateien) | Player, Physik-, Audio-Bibliotheken, native Plugins, IL2CPP | **immer wenn möglich** disassemblieren und nach IR heben (`frontend-native`, ADR-023), Rückfall: Handarbeit mit Konformitätstests | M10b |
| Dokumentation und Beobachtung | Beschreibung, aufgezeichnete Läufe | Verhaltensvertrag und Konformitätstests, Rumpf von Hand | laufend |

Wichtig: Auch bei Übersetzung aus Binärdateien landet das Ergebnis nicht unkontrolliert im Core. Fremde Begriffe bleiben im Adapter, neutrale Fähigkeiten gehen als Provider hinter `engine-api` (Regel 12/13 in `AGENTS.md`). Übersetzte Engine-Module sind **generiert** (wie `gen/`), Handarbeit liegt daneben in `manual/` der Engine-Recomp.

### Funktionale Gleichheit messen

Gleichheit wird nie behauptet, sondern gemessen. Die Engine-Recomp enthält dafür einen Ordner `conformance/`:

```
engines/unity/
  port.toml
  conformance/
    scenarios/        Szenarien: Eingabeskript + Startszene (neutrales Format)
    reference/        Referenzspuren (Traces), aufgezeichnet vom Original
    tolerances.toml   erlaubte Abweichungen je Größe
  adapter-unity/
  ...
```

- **Szenario:** deterministischer Eingabeskript-Lauf (Frames, Eingaben, feste Zeitschritte) auf einer Mini-Szene.
- **Trace:** Folge von Beobachtungen pro Frame, z. B. Transformwerte, Lifecycle-Aufrufreihenfolge, Physik-Kontakte, Events. Das Format ist textuell, deterministisch und diffbar.
- **Referenz:** entsteht durch `port conformance record` gegen das Original (laufende Engine oder deren Binärdateien in einer Testumgebung) und wird als Golden File committet. Es gibt Ebenen: API-Ebene (einzelne Funktionen mit Eingaben/Ausgaben), Lifecycle-Ebene, Szenenebene.
- **Vergleich:** `port conformance run` spielt dasselbe Szenario gegen die Recomp und vergleicht Trace und Referenz mit den Toleranzen. Ergebnis: bestanden/abweichend je Szenario, mit der ersten abweichenden Stelle.
- **Fortschritt:** Konformitätsrate (bestandene Szenarien je Subsystem) ergänzt die API-Abdeckung als Maß für eine Engine-Recomp.

Abweichungen sind erlaubt, wenn sie in `tolerances.toml` begründet stehen (z. B. Gleitkommatoleranz, Physik-Reihenfolge). Alles andere ist ein Fehler.

### Pipeline für Engine-Module aus IL
```
Engine-Binary -> [extract] -> [frontend] -> [ir] -> [fix, optional] -> [gen] -> Engine-Modul
                                                         \-> [conformance run] gegen Referenz
```
Gleiche Phasen wie bei Spielen, aber: Index und Lint nutzen Engine-Regeln (z. B. keine Abhängigkeit auf Plattform-APIs im Modul), und `gen` schreibt in das Maven-Modul der Engine-Recomp. Über `port -C engines/unity ...` läuft derselbe CLI.
