# Modul `engine-core`

## Zweck
Implementierung der eigenen Engine hinter `engine-api`. Neutral: kennt weder Unity noch Godot.

## Abhängigkeiten
`engine-api`. Sonst nichts aus dem Projekt. Niemals Adapter oder Frontends.

## Bestandteile
- **Welt/Entity-Verwaltung** (Form nach ADR-006, hinter Interface).
- **Scheduler** mit Phasen `EARLY`, `FIXED`, `UPDATE`, `LATE`. Fixed-Timestep ist konfigurierbar. Reihenfolge innerhalb einer Phase ist deterministisch (Registrierungsreihenfolge, dokumentiert).
- **Transform-System:** lokale/globale Transformationen, Eltern-Kind-Beziehungen, rechts-händig Y oben (ADR-007).
- **Input:** abstrahierte Achsen und Tasten, im Test per Skript injizierbar (`ScriptedInput`).
- **Rendering:** zunächst nur `NullRenderer` (ADR-014). Kein Grafikcode.
- **Headless-Betrieb:** `Engine.step(n)` führt n Frames mit festem Zeitschritt aus, ohne Fenster und ohne Wanduhr.

- **Subsystem-Verwaltung (ADR-015):** Registry für Provider (zunächst fest verdrahtete Standard-Provider, später `ServiceLoader`), `EventBus`, Prüfung von `SubsystemInfo` (ein Besitzer pro Komponente, Pflichtpaare, Konflikte). Subsysteme importieren sich nie gegenseitig.

## Datenmodell (ADR-016, PROPOSED)

- **Entity:** 64-Bit-ID, Index in den unteren, Generation in den oberen Bits. Zugriff mit veralteter Generation wirft eine klare Exception.
- **`ComponentStore<T>`:** Interface in `engine-api`, Implementierung `SparseSetStore<T>` in `engine-core` (dichtes Array plus Index-Tabelle). Iteration in aufsteigender Entity-Reihenfolge.
- **Command-Buffer:** Erzeugen/Zerstören von Entities und Hinzufügen/Entfernen von Komponenten während einer Iteration werden gesammelt und am Phasenende in Registrierungsreihenfolge ausgeführt.
- **Hierarchie:** `Parent(entity)` und geordnete `Children`. Zerstören eines Elternobjekts zerstört die Kinder (dokumentiert, getestet). `TransformSystem` berechnet `WorldTransform` einmal pro Frame in der Phase `LATE` oder bei Bedarf (festlegen und dokumentieren).
- **Erweiterungspunkt:** Adapter registrieren eigene Komponententypen (z. B. `ScriptHost`). Der Core speichert sie, ohne ihren Inhalt zu kennen.
- **Debugbarkeit:** `World.dump()` (deterministisch, stabile Reihenfolge) und `EntityView` (liefert alle Komponenten einer Entity lesbar).

## Verhalten
- Gleiche Eingabe (Szene, Eingabeskript, Zeitschritt) ergibt identischen Zustand nach n Frames (Determinismus).
- Der Core trifft keine Annahmen über Unity-Lifecycle (`Awake`/`Start`). Das macht der Adapter über Scheduler-Phasen.

## Tests
- Scheduler: Phasenreihenfolge, Fixed-Timestep-Akkumulator, Determinismus.
- Transform: Eltern-Kind, Rotation/Skalierung, Koordinatenkonvention.
- Headless-Lauf mit skriptierter Eingabe.
- ArchUnit: kein Import aus `adapter-*`, `frontend-*`, `ir`.

## Nicht-Ziele
- Kein Fenster, kein echtes Rendering, keine Physik, kein Audio (spätere Meilensteine).
- Keine fremden Engine-Konzepte (`GameObject`, `Node`, `MonoBehaviour`).
