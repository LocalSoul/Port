# Modul `adapter-unity`

## Zweck
Bildet die benötigte Teilmenge der UnityEngine-API (Namespace `UnityEngine`) auf die Engine-API ab. Generierter Spielcode spricht nur mit diesem Modul.

## Abhängigkeiten
`engine-api`. Nie `engine-core`.

## Aufgaben
1. **API nachbilden:** `MonoBehaviour`, `Component`, `GameObject`, `Transform`, `Vector3`, `Quaternion`, `Time`, `Input` (zunächst Minimalumfang, wächst bedarfsgesteuert).
2. **Semantik umsetzen:** Lifecycle (`Awake`, `Start`, `Update`, `FixedUpdate`, `LateUpdate`) über die Scheduler-Phasen der Engine-API. Koordinatenkonvertierung (Unity links-händig → Core rechts-händig, ADR-007) ausschließlich hier.
3. **Assets laden:** Loader für Szenen/Prefabs in das neutrale Datenformat (ab M8, ADR-010).
4. **Metadaten liefern:** `adapter-metadata.json` im Artefakt, Pro API-Symbol Status `full`, `stub` oder `missing`.

## Stand M3
- Modul `engines/unity/adapter-unity` (Maven-Modul `adapter-unity`), Paket `io.github.localsoul.adapter.unity`. Abhängig nur von `engine-api`, `engine-core` nur im Test-Scope (Enforcer, ArchUnit).
- Typen: `UnityObject` (für `UnityEngine.Object`, um nicht mit `java.lang.Object` zu kollidieren), `Component`, `Behaviour`, `MonoBehaviour`, `GameObject`, `Transform` (`position()`, `translate(Vector3)`), `Vector3` (Record mit `mul`, `add`, `sub`, `equals` mit Unitys Toleranz), `Time` (`deltaTime()`, `fixedDeltaTime()`), `Input` (`getAxis`, Status `stub`: Rohwert ohne Glättung).
- `UnityRuntime.install(engine)` registriert die Lifecycle-Callbacks und hält die aktive Engine und den Kontext des laufenden Phasenschritts statisch (die Unity-API ist statisch). Der Kontext gilt bis zum Ende des `LATE`-Callbacks des Adapters; außerhalb liefern `Time`/`Input` 0.
- `GameObject`: Entity mit Core-`Transform` und der adapter-eigenen Komponente `ScriptHost` (Skripte, ADR-016). `addComponent(instanz)` statt `AddComponent<T>()` (generischer Weg folgt mit dem Mapping in M3a).
- `Transform.translate` (`Space.Self`) ist korrekt für Objekte ohne rotiertes oder skaliertes Elternobjekt.
- Koordinaten (ADR-007): Spiegelung der Z-Achse, nur im Adapter, mit `0f - z` (kein `-0.0`).

## Namensabbildung (Konvention für den Generator)
- Typen behalten ihren Namen (`UnityEngine.Transform` → `io.github.localsoul.adapter.unity.Transform`), Ausnahme `UnityEngine.Object` → `UnityObject`.
- Methoden und Properties werden `lowerCamelCase`: `Update` → `update`, `transform` (Property) → `transform()`, `deltaTime` (statisch) → `Time.deltaTime()`.
- Operatoren auf Wertetypen sind statische Methoden: `op_Multiply` → `Vector3.mul(...)`, `op_Addition` → `add`, `op_Subtraction` → `sub`, `op_Equality` → `equals`.

Die vollständige Mapping-Tabelle liegt als Datei `engines/unity/adapter-unity/src/main/resources/mapping.tsv` (Spalten: IR-Symbol, Java-Symbol, Form; Formen siehe `docs/modules/generator-java.md`) und wird vom Generator gelesen. Lifecycle-Methoden stehen als Form `message` am Typ `UnityEngine.MonoBehaviour`. Der Adapter ist die Quelle der Wahrheit für dieses Mapping; Typzeilen zählen mit. Ein Test prüft Mapping ↔ Metadaten und dass jedes Mapping-Ziel existiert.

## Metadaten-Format
```json
{
  "schema": "adapter-metadata/1",
  "adapter": "unity",
  "version": "0.1.0",
  "symbols": [
    { "id": "UnityEngine.Transform::Translate#UnityEngine.Vector3", "status": "full" },
    { "id": "UnityEngine.Physics::Raycast#UnityEngine.Vector3,UnityEngine.Vector3", "status": "missing" }
  ]
}
```
Sortiert nach `id`. Ein Test stellt sicher, dass das Mapping und die Metadaten konsistent sind (jedes `full`/`stub`-Symbol hat einen Mapping-Eintrag).

## Lifecycle-Referenz (Unity, vereinfacht)

Grundlage für die Lifecycle-Tests. Quelle: Unity-Dokumentation „Execution Order of Event Functions". Ist ein Detail unklar, nicht raten, sondern als Frage in `PROGRESS.md` festhalten.

| Zeitpunkt | Reihenfolge |
|---|---|
| Objekt wird aktiv erzeugt | `Awake` → `OnEnable` → (vor dem ersten `Update` des Skripts) `Start` (einmalig) |
| Pro Frame | `FixedUpdate` (0..n mal) → `Update` → Coroutinen-Fortsetzung (`yield return null`, nach `Update`) → `LateUpdate` |
| Objekt wird zerstört | `OnDisable` → `OnDestroy` |

Abbildung auf Core-Phasen (Stand M3, korrigiert gegenüber dem ersten Entwurf): `Awake` und `OnEnable` laufen **sofort** bei `addComponent` (wie in Unity), `Start` in `EARLY` vor dem ersten `Update` des Skripts, `FixedUpdate` in `FIXED`, `Update` (und später Coroutinen-Fortsetzung) in `UPDATE`, `LateUpdate` in `LATE`. Reihenfolge mehrerer Skripte: Entities aufsteigend, je Entity in Anhängereihenfolge (Unity selbst definiert sie nicht streng). Skripte, die während eines Phasenschritts hinzukommen, laufen ab dem nächsten Schritt.

Zeit: `Time.deltaTime` ist das Frame-Delta (innerhalb von `FixedUpdate` entspricht es `Time.fixedDeltaTime`), `Time.fixedDeltaTime` ist standardmäßig 0,02 s, `Time.timeScale` skaliert beides. Abweichungen werden im Test und hier dokumentiert. Stand M3: `Time.timeScale` fehlt noch.

## Verhältnis zu Subsystem-Providern (ADR-015)

Der Adapter bestimmt die **API-Semantik**, die das Spiel erwartet. Subsystem-Provider liefern nur Rohdaten und Verhalten.
- Beispiel: Die Unity-Glättung bei `Input.GetAxis` (Sensitivity, Gravity) lebt im Adapter. Der gewählte Input-Provider liefert Rohwerte und Ereignisse.
- Der Adapter darf nicht von einem bestimmten Provider abhängen, nur von den Subsystem-Interfaces und Komponentenschemas der Engine-API.

## Tests
- Lifecycle-Reihenfolge gegen eine Referenzabfolge.
- Koordinatenkonvertierung (Translate in Unity-Koordinaten ergibt erwartete Core-Position).
- Mapping-/Metadaten-Konsistenz.
- Alles headless gegen `engine-core` als Test-Abhängigkeit (nur im `test`-Scope).

## Nicht-Ziele
- Kein Nachbau der gesamten UnityEngine-API. Es zählt, was tatsächlich genutzt wird (`port deps check`).
- Keine Editor-Funktionen, keine Unity-spezifischen Serialisierungsdetails außerhalb des Loaders.
