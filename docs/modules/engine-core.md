# Modul `engine-core`

## Zweck
Implementierung der eigenen Engine hinter `engine-api`. Neutral: kennt weder Unity noch Godot, keine Skripte.

## Abhängigkeiten
Nur `engine-api` (Maven Enforcer). ArchUnit (`ArchitectureTest`) prüft: nur JDK, `engine.api` und `engine.core`; nichts aus `ir`, `adapter`, `frontend`; keine Reflection, keine Konsolenausgabe; die Pakete unter `engine.core.subsystems.*` hängen nicht voneinander ab und nutzen nur die API.

## Bestandteile (Stand M1)

| Paket | Inhalt |
|---|---|
| `engine.core` | `CoreEngine` (Builder: `fixedDelta`, `frameDelta`, `subsystem`), `FrameHooks` (Ablauf je Phasenschritt) |
| `engine.core.schedule` | `Scheduler`, `PhaseHooks`, `FrameContextFactory` |
| `engine.core.world` | `CoreWorld`, `SparseSetStore`, `EntityTable`, `CommandBuffer` |
| `engine.core.transform` | `TransformSystem` |
| `engine.core.event` | `CoreEventBus`, `DeclaredEventBus` |
| `engine.core.input` | `InputView` (Lesesicht auf `InputState`, kein Subsystem) |
| `engine.core.subsystem` | `SubsystemRegistry` (Startprüfung) |
| `engine.core.subsystems.input` | `ScriptedInput`, `InputScript` (Standard-Provider `input-scripted`), `KeyboardInput` (`input-keyboard`, SC) |
| `engine.core.subsystems.render` | `NullRenderer` (Standard-Provider `render-null`), `SpriteRenderSystem` (`render-sprite2d`, SC) |

## Frame-Ablauf
1. `EARLY` → `FIXED` × n → `UPDATE` → `LATE`. Jeder einzelne Durchlauf ist ein **Phasenschritt**.
2. Je Phasenschritt:
   1. nur in `LATE`: `TransformSystem` schreibt `WorldTransform`,
   2. Sammeln der Strukturänderungen beginnt,
   3. Subsysteme laufen (Registrierungsreihenfolge),
   4. Callbacks laufen (Registrierungsreihenfolge),
   5. Strukturänderungen werden ausgeführt, Event-Puffer getauscht.
3. **Fixed-Timestep:** Akkumulator in `double`. Ein `FIXED`-Schritt läuft, solange der Akkumulator mindestens `fixedDelta · (1 − 10⁻⁴)` enthält. Die Toleranz gleicht `float`-Rundung der Frame-Dauer aus (`2.5f * 0.02f` liegt knapp unter 2,5 Schritten), der Rest bleibt für den nächsten Frame.
4. `Time.delta` ist in allen Phasen die Frame-Dauer, `Time.frame` beginnt bei 0. Keine Wanduhr: `step(n)` nutzt die konfigurierte Frame-Dauer. `stepFrame(delta)` (SC) führt einen Frame mit vorgegebener Dauer aus; die Echtzeit-Schleife mit Wanduhr liegt in `launcher-boot`, nicht im Core. `requestQuit()` setzt nur eine Anforderung, die der Start auswertet.

## Datenmodell (ADR-016, ACCEPTED)
- **Entity:** 64-Bit-ID, Index unten, Generation oben. Freie Indizes werden FIFO wiederverwendet, die Generation steigt beim Zerstören.
- **`SparseSetStore<T>`:** dichtes Array **sortiert nach Entity-Index**, Index-Tabelle für O(1)-Zugriff. Einfügen und Entfernen sind O(n) (`arraycopy`), dafür ist die Iteration deterministisch aufsteigend.
- **Command-Buffer:** siehe `docs/modules/engine-api.md` (Phasenende, Prüfung gegen erwarteten Zustand). Widersprüchliche `setParent` in derselben Phase, die erst zusammen einen Zyklus ergeben, werfen am Phasenende `HierarchyException`, nachdem alle übrigen Befehle ausgeführt sind.
- **Hierarchie:** `Parent` und geordnete `Children` werden nur über `setParent` gepflegt. `destroy` zerstört den Teilbaum, Kinder vor dem Elternobjekt.
- **Transform:** `worldMatrix(e)` rechnet auf Abruf (`Eltern · T · R · S`). Entities ohne `Transform` zählen als Identität. `TransformSystem` schreibt `WorldTransform` zu Beginn von `LATE` und entfernt veraltete Einträge. Aufwand O(n · Tiefe), ein Cache ist ohne API-Änderung möglich.
- **Dump:** `world entities=N`, dann je Entity `entity index:generation` und je Komponente `  Einfachname toString()`. Entities aufsteigend, Komponenten nach vollem Klassennamen sortiert, genau ein abschließendes `\n`.

## Standard-Provider
- `ScriptedInput` (`input-scripted`, Art `input`, schreibt `InputState`): legt beim Start eine Entity mit `InputState` an und wendet in `EARLY` die Skriptereignisse des Frames an. `pressed`/`released` gelten nur in diesem Frame, Achsen bleiben bis zur Änderung bestehen.
- `NullRenderer` (`render-null`, Art `render`, liest `WorldTransform`, `Renderable`): zählt in `LATE` Frames und sichtbare Renderables. Er läuft vor den `LATE`-Callbacks, deren Änderungen sieht er erst im nächsten Frame.
- `KeyboardInput` (`input-keyboard`, Art `input`, konsumiert `input.key`, schreibt `InputState`): setzt in `EARLY` die Tastenereignisse des Busses in gehaltene, gedrückte und losgelassene Tasten um. Da Ereignisse erst im nächsten Phasenschritt sichtbar sind, wirkt ein Tastendruck im Frame nach dem Abholen durch den Renderer.
- `SpriteRenderSystem` (`render-sprite2d`, Art `render`, ADR-014): zeichnet in `LATE` über ein `RenderBackend`. Kamera: erste `Camera2D` mit `WorldTransform` (sonst Standard, schwarz). Sprites mit sichtbarer Textur und `WorldTransform`, sortiert nach `order`, dann Entity-Index; Abbildung Welt → Pixel als `Affine2D` (Translation, Drehung um z, Skalierung, Pivot, y gespiegelt). Danach jede `UiCanvas` in Bildschirmpixeln. Schreibt `Viewport` bei Größenänderung und veröffentlicht die Tasten des Backends als `input.key`. Läuft vor den `LATE`-Callbacks: was ein Adapter dort setzt (z. B. die UI von `OnGUI`), erscheint einen Frame später.
- `InputView` liest die erste Entity mit `InputState`. Ohne Input-Subsystem ist alles 0 bzw. nicht gedrückt.

## Verhalten
- Gleiche Eingabe (Szene, Eingabeskript, Zeitschritt) ergibt identischen Zustand und Dump nach n Frames (Tests über 100 und 1000 Frames).
- Der Core trifft keine Annahmen über Unity-Lifecycle (`Awake`/`Start`). Das macht der Adapter über Scheduler-Phasen.

## Tests
- Scheduler: Phasenreihenfolge, Akkumulator (auch 1000 Frames mit gerundeter Frame-Dauer), Determinismus.
- Welt: Sparse-Set, Generationen, Command-Buffer während Iteration, Kaskade, Dump-Format und Determinismus.
- Transform: Eltern verschieben, drehen und skalieren, `WorldTransform` in `LATE`.
- Headless: Mini-Szene 100 Frames, Eingabeskript, identische Dumps, Lauf über Subsysteme.
- Rendering (SC): aufzeichnendes Test-Backend (`RecordingBackend`), Kamera, Sortierung, Pivot, UI, Tasten → `InputState`.
- Subsysteme: doppelter Schreiber, Core-Besitz, `requires`, `conflictsWith`, doppelte IDs, Event-Sichtbarkeit über 1000 Frames.

## Nicht-Ziele
- Kein Fenster und keine Grafik-Bibliothek im Core: Ausgabe nur über `RenderBackend` (z. B. `render-swing`). Keine Physik, kein Audio (spätere Meilensteine).
- Keine fremden Engine-Konzepte (`GameObject`, `Node`, `MonoBehaviour`).
