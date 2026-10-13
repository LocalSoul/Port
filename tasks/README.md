# Aufgaben

Jede Aufgabendatei beschreibt einen Meilenstein in kleinen, einzeln abnehmbaren Schritten.

## Aufbau einer Aufgabe

- **ID** (z. B. `M0.3`), **Ziel**, **Eingaben** (Dokumente, Dateien), **Schritte**, **Abnahmekriterien** (prüfbar, jeweils mit zugehörigem Test), **Nicht-Ziele**, **Abhängigkeiten/Entscheidungen**.
- Abnahmekriterien sind Checkboxen. Eine Aufgabe ist erst fertig, wenn alle abgehakt sind.

## Reihenfolge

`M0` → `M1` → `M2` → `M3` → `M4` → `M4a`. Innerhalb eines Meilensteins in Nummernreihenfolge, außer eine Aufgabe nennt etwas anderes.

## Dateien

| Datei | Meilenstein |
|---|---|
| `M0-ir.md` | Repo-Skelett, IR-Parser, Printer, Validator |
| `M1-engine-core.md` | Engine-API und Engine-Core (headless) |
| `M2-unity-frontend.md` | Unity-Frontend (C# → IR) |
| `M3-generator-adapter.md` | Java-Generator und `adapter-unity` |
| `M4-cli-skeleton.md` | CLI-Grundgerüst |
| `M4a-launcher.md` | Monorepo-Layout, Katalog, Launcher (headless) |

M3a und M5 bis M12a (inklusive M6a und M7a) stehen in `docs/roadmap.md` und werden detailliert, wenn sie dran sind.

## Hinweis für Agents

Vor Beginn einer Aufgabe: Abschnitt „Abhängigkeiten/Entscheidungen" lesen. Steht dort ein `OPEN`-ADR, nicht selbst entscheiden (siehe `AGENTS.md`).
