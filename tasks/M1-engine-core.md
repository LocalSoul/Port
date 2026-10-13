# M1: Engine-API und Engine-Core (headless)

**Ziel:** Eine lauffähige, deterministische, headless Engine mit Scheduler-Phasen, Transform-System und skriptierbarer Eingabe. Sie zeigt, welche Teile die Engine-API wirklich braucht.

**Eingaben:** `AGENTS.md`, `docs/architecture.md`, `docs/modules/engine-api.md`, `docs/modules/engine-core.md`, `docs/decisions.md` (ADR-006, 007, 011, 014).

**Abhängigkeiten/Entscheidungen**
- **ADR-006/ADR-016 (Datenmodell) sind `PROPOSED`:** Entwurf ist ein ECS mit Sparse-Set-Speicher, Entity-IDs mit Generation, Command-Buffer, `Parent`/`Children`, Skripte nur im Adapter. Du darfst dem Entwurf folgen, implementiere aber ausschließlich hinter den Interfaces in `engine-api` und weise im Bericht darauf hin. Widersprechen deine Erkenntnisse dem Entwurf, anhalten und den Nutzer fragen.
- **ADR-007 (Koordinaten)** ist `PROPOSED`: rechts-händig, Y oben, Meter.
- **ADR-014 (Rendering)** ist `OPEN`: nur `NullRenderer`.
- **ADR-015 (Subsystem-Provider)** ist `PROPOSED`: Subsysteme getrennt halten, Event-Bus und `SubsystemInfo` einführen (M1.5a). Mehrere Provider und `port engine`-Befehle kommen erst in M12.

---

## M1.1 Vorschlag für das Datenmodell (ADR-006)

**Schritte**
1. Schreibe einen kurzen Vergleich ECS vs. Entity-Baum, jeweils mit: Abbildung von Unity (GameObject + Components, Eltern-Kind), Abbildung von Godot (Node-Baum), Aufwand, Debugbarkeit, Performance-Erwartung.
2. Lege ihn in `docs/decisions.md` unter ADR-006 als Optionen mit Empfehlung ab.
3. Prüfe, ob deine Erkenntnisse ADR-016 (ECS-Entwurf, `PROPOSED`) widerlegen. Wenn nicht, folge ihm hinter den Interfaces. Wenn ja, anhalten und den Nutzer fragen.

**Abnahme**
- [x] ADR-006 enthält beide Optionen, Kriterien und eine Empfehlung.
- [x] Der Nutzer hat entschieden, oder der Bericht nennt klar, dass die Implementierung vorläufig ist.

## M1.2 Module und Mathe

**Schritte**
1. Module `engine-api` und `engine-core` anlegen, ArchUnit-Regeln aus `AGENTS.md`/`architecture.md` eintragen.
2. Mathe-Wertetypen in `engine-api`: `Vec2`, `Vec3`, `Quat`, `Mat4` (unveränderlich, rechts-händig Y oben).
3. Operationen: Addition, Skalierung, Skalarprodukt, Kreuzprodukt, Länge, Normalisierung, Quaternion-Multiplikation, Rotation eines Vektors, Matrix-Komposition aus Position/Rotation/Skalierung.

**Abnahme**
- [x] Tests für alle Operationen inklusive Randfälle (Nullvektor, Einheitsquaternion, Gleitkommatoleranz mit definiertem Epsilon).
- [x] Kreuzprodukt von X und Y ergibt +Z (rechts-händig).
- [x] Alle Typen dokumentieren Einheiten und Konvention im Javadoc.

## M1.3 Scheduler

**Schritte**
1. `Phase`: `EARLY`, `FIXED`, `UPDATE`, `LATE`.
2. Registrierung von Callbacks pro Phase mit definierter, deterministischer Reihenfolge (Registrierungsreihenfolge).
3. Fixed-Timestep-Akkumulator: pro Frame 0..n `FIXED`-Aufrufe, Zeitschritt konfigurierbar.
4. `Time`-Werte (Delta, Fixed-Delta, Frame-Nummer) abrufbar.

**Abnahme**
- [x] Reihenfolge innerhalb einer Phase ist wie registriert (Test).
- [x] Reihenfolge der Phasen pro Frame: `EARLY` → `FIXED`* → `UPDATE` → `LATE` (Test).
- [x] Bei Frame-Delta = 2,5 × Fixed-Delta werden zwei `FIXED`-Aufrufe ausgeführt und der Rest bleibt im Akkumulator (Test).
- [x] Gleiche Eingabe ergibt gleiche Aufrufabfolge (Determinismus-Test über 1000 Frames).

## M1.4 Welt, Entities, Transform

**Voraussetzung:** ADR-016 gelesen (ECS, Sparse-Set, `PROPOSED`), siehe auch `docs/modules/engine-core.md` und `docs/modules/engine-api.md`.

**Schritte**
1. `World`, `Entity`, `Component` laut Interfaces in `engine-api`.
2. Transform-System: lokale Position/Rotation/Skalierung, Eltern-Kind, globale Transformation abfragbar.
3. Erweiterungspunkt für eigene Komponententypen (Registrierung, Abfrage nach Typ).
4. Entities erzeugen und zerstören, Zerstörung wirkt am Phasenende (ADR-016, vom Nutzer entschieden; dokumentiert, getestet).
5. Entity-IDs mit Generation, `ComponentStore` als Sparse-Set, Command-Buffer für Strukturänderungen während der Iteration, Iteration in aufsteigender Entity-Reihenfolge.
6. `Parent`/`Children`, Zerstören eines Elternobjekts zerstört die Kinder.
7. `World.dump()` (deterministisch) und `EntityView` für Debugging.
8. Keine Skript- oder Unity-/Godot-Begriffe im Core. Adapterdefinierte Komponententypen laufen über `registerComponent`.

**Abnahme**
- [x] Verschobenes Elternobjekt verschiebt Kind in globalen Koordinaten (Test).
- [x] Rotation des Elternobjekts dreht Kind um Elternursprung (Test).
- [x] Eigene Komponente registrieren, an Entity hängen, abfragen (Test).
- [x] Entity zerstören während eines Frames: Verhalten laut Dokumentation (Test).
- [x] Zugriff mit veralteter Entity-Generation wirft eine klare Exception (Test).
- [x] Hinzufügen/Entfernen einer Komponente während der Iteration führt nicht zu Fehlern und wirkt erst am Phasenende (Test).
- [x] Zerstören des Elternobjekts zerstört die Kinder (Test).
- [x] `World.dump()` ist für gleiche Eingabe byte-identisch (Test).

## M1.5 Input und Headless-Betrieb

**Schritte**
1. `Input`-Interface in `engine-api` (Achsen, Tasten gedrückt/gehalten/losgelassen).
2. `ScriptedInput` in `engine-core`: Eingaben pro Frame aus einem Skript (Liste von Frame → Ereignisse).
3. `Engine.step(n)`: n Frames mit festem Zeitschritt, ohne Fenster, ohne Wanduhr.
4. `NullRenderer` hinter `Renderer`-Interface.

**Abnahme**
- [x] Mini-Szene (eine Entity mit einer Komponente, die per Update die Position ändert) läuft 100 Frames und endet im erwarteten Zustand.
- [x] Zwei Läufe mit gleichem Skript ergeben bytegleiche Zustandsdumps (Dump-Format deterministisch).
- [x] Skriptierte Eingabe wird in der richtigen Phase sichtbar (Test).

## M1.5a Subsystem-Vertrag und Event-Bus (ADR-015)

**Voraussetzung:** M1.4 und M1.5. Form der Komponenten hängt an ADR-006.

**Schritte**
1. In `engine-api`: `Subsystem`, `SubsystemInfo`, `ComponentType`, `EventType`, `EventBus` laut `docs/modules/engine-api.md`.
2. Komponentenschemas Version 1 (klein halten): `Transform`, `Renderable` (Platzhalter), `InputState`. Felder mit dokumentierten Defaults.
3. In `engine-core`: Event-Bus (gepuffert pro Phase, deterministische Reihenfolge), Registry für Standard-Provider, Start-Prüfung von `SubsystemInfo`.
4. `ScriptedInput` und `NullRenderer` implementieren `Subsystem` und deklarieren, was sie lesen und schreiben.
5. Regel: Kein Subsystem importiert ein anderes. Kommunikation nur über Welt-Komponenten und Events.

**Abnahme**
- [x] ArchUnit-Test: Subsystem-Implementierungen importieren sich nicht gegenseitig.
- [x] Start mit zwei Subsystemen, die dieselbe Komponente schreiben, schlägt mit klarer Fehlermeldung fehl (Test).
- [x] Start mit nicht erfülltem `requires` schlägt mit klarer Fehlermeldung fehl (Test).
- [x] Ereignisse eines Subsystems sind im nächsten Phasenschritt für Konsumenten sichtbar, Reihenfolge deterministisch (Test über 1000 Frames).
- [x] Mini-Szene aus M1.5 läuft unverändert über die neue Subsystem-Struktur.

## M1.6 Abschluss

**Abnahme**
- [x] ArchUnit: `engine-core` importiert nichts aus `adapter-*`, `frontend-*`, `ir`.
- [x] Offene Wünsche an die Engine-API stehen in `PROGRESS.md` (Nebenfunde).
- [x] `docs/modules/engine-api.md` und `engine-core.md` entsprechen der Implementierung.

## Nicht-Ziele
Fenster, echtes Rendering, Physik, Audio, Unity-/Godot-Begriffe im Core.
