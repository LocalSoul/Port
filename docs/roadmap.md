# Roadmap

Meilensteine werden in dieser Reihenfolge bearbeitet. Jede Zeile nennt Inhalt und **Abnahme** (messbar). Detaillierte Aufgabendateien gibt es für M0 bis M4 in `tasks/`. Spätere Meilensteine werden detailliert, wenn sie dran sind.

| ID | Inhalt | Abnahme |
|---|---|---|
| M0 | Repo-Skelett, IR-Parser, Printer, Validator mit Roundtrip-Tests | Beispiel aus der IR-Spec parst, wird kanonisch gedruckt, Roundtrip stabil; Validator meldet IR001–IR005 mit Position |
| M1 | Engine-API und Engine-Core-Gerüst (headless) | Mini-Szene läuft N Frames deterministisch; Scheduler-Phasen testbar; Transform-Mathe getestet |
| M2 | Unity-Frontend: C#-Assembly → IR | Synthetisches Testspiel (aus Stubs kompiliert) erzeugt erwartete IR (Golden Files) |
| M3 | Java-Generator und erste `adapter-unity`-Version | `Mover`-Beispiel läuft End-to-End headless: IR → Java → kompiliert → bewegt Transform wie erwartet |
| M3a | `runtime-system` (BCL-Nachbau, Minimalumfang) und Typabbildung (ADR-019) | `List<T>`, `Dictionary<K,V>`, `string`-Hilfen, `Math`-Grundfunktionen, Basis-Exceptions mit Tests der Semantikunterschiede; Generator nutzt die `mapping.tsv`; Fixture mit Collections läuft End-to-End |
| M4 | CLI-Grundgerüst | `init`, `extract`, `ir`, `gen`, `build`, `status` laufen auf dem Testspiel, `--json` und Exit-Codes nach Vertrag |
| M4a | Monorepo-Layout, Katalog und Launcher (headless), ADR-020 | `games/` und `engines/` im Repo, `port init --kind`, `port catalog list/show`, `port launch new/check/run` laufen mit dem Testspiel und `engines/unity`; Kompatibilitätsfälle getestet |
| M5 | Index (SQLite) und Suchbefehle | `find`, `refs`, `callers`, `callees`, `uses`, `show`, `outline`; inkrementelle Aktualisierung; stabile Symbol-IDs |
| M6 | Linter mit Kernregeln und Adapter-Metadaten | IR001–IR005, PT001–PT003, SM001, SH001/SH002, MN001–MN003 mit Tests; Regeln per `port.toml` konfigurierbar |
| M6a | Agent-first Tooling (ADR-018) | `schema`, `explain`, `task next/list`, `verify --scope`, `patch apply/preview`, `--dry-run`, Paginierung, JSON als Standard bei nicht-interaktiver Ausgabe; `port mcp` mit lesenden Tools; Schema-Tests und Golden Files |
| M7 | `ir --update` mit 3-Wege-Merge | Neue Spielversion wird gemergt, Konflikte erzeugen Exit 6, Overrides werden auf Gültigkeit geprüft |
| M7a | KI-Fix-Phase (ADR-017) | `port fix` mit Fake-Agent getestet: Overlay in `fixes/`, Prüfung vor Annahme, Zwischenspeicher, `stale`-Erkennung (FX001–FX003), Review-Befehle, `propose_fix`/`accept_fix`/`reject_fix` im MCP-Server (nur mit `--allow-write`) |
| M8 | Asset-Pipeline | Szenen und Prefabs eines Testspiels werden extrahiert und vom Unity-Adapter geladen |
| M9 | Erstes echtes, kleines 2D-Spiel (eigenes) end-to-end | Spiel startet auf der eigenen Engine, Kernspielschleife funktioniert (abhängig von ADR-013) |
| M10 | Physik, Audio, Coroutines ausbauen; zweites Spiel; GraalVM-Native-Image | Referenzszenen laufen, CLI startet als Native Image |
| M10b | Natives Frontend (ADR-023): Disassemblieren und Heben nach IR | Synthetische native Fixtures (kleine C-Funktionen) werden gehoben, nach Java übersetzt und bestehen Differenztests; Status `lifted`/`lifted-low`/`manual` pro Funktion; Lint `NT001`–`NT003`; IR-Spec-Erweiterung freigegeben |
| M11 | Zweiter Adapter (Godot) mit eigenem Frontend und Loader | Referenzszene verhält sich ähnlich zum Original, ADR-012 entschieden |
| M11a | Engine-Recomp aus verwalteten Binärdateien und Konformitätstests (ADR-022) | Ein kleiner Teil einer Engine-Assembly (z. B. Mathe-Typen) wird über die Pipeline in ein Engine-Modul übersetzt; `port conformance run` besteht Szenarien gegen eine aufgezeichnete Referenz, Toleranzen dokumentiert |
| M12 | Subsystem-Provider und Mischbarkeit (ADR-015) | Mindestens zwei Provider für Input (z. B. `unity-like`, `godot-like`) und ein austauschbarer zweiter Provider für ein weiteres Subsystem; `port engine list/info/set/check`; Auswahl über `port.toml`; Referenzszene läuft mit mindestens zwei Kombinationen |
| M12a | Launcher-Oberfläche (ADR-021) | Spiel, Engine und Subsysteme per Oberfläche wählen, Profil speichern, Spiel starten; nur `launcher-core` wird aufgerufen |

## Reihenfolge-Hinweise

- **M1 vor M2:** Ohne laufenden Core siehst du nicht, welche Teile die Engine-API wirklich braucht.
- **M0 zuerst:** Die IR ist der Vertrag zwischen allen Teilen. Parser und Printer müssen vorher stabil sein.
- Abhängige Entscheidungen: M1 (ADR-006, 007, 014), M3 (ADR-008, 009), M8 (ADR-010), M9 (ADR-013), M11 (ADR-012), M12 (ADR-015), M4a (ADR-020), M11a (ADR-022), M10b (ADR-023), M12a (ADR-021), M6a (ADR-018), M7a (ADR-017), M3a (ADR-019).
- M6a baut auf dem Index (M5) und dem Linter (M6) auf, M7a auf M6a und M7. Details: `docs/agent-tooling.md`, `docs/ai-fix-phase.md`.
- Die Grundlagen für M12 entstehen schon in M1: getrennte Subsystem-Interfaces, Event-Bus, `SubsystemInfo`, Komponentenschemas.
- M4a baut auf dem lauffähigen Spiel-Pfad (M4) auf, damit der Launcher von Anfang an ein echtes Profil starten kann. Weitere Spiele und weitere Engine-Recomps (z. B. M11) werden einfach als neue Unterordner angelegt.
- Game-Recomp und Engine-Recomp sind getrennte Arbeitsstränge: Fortschritt bei Spielen (offene `@manual`) und bei Engines (API-Abdeckung) wird getrennt gemessen.
