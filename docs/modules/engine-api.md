# Modul `engine-api`

## Zweck
Schmale, versionierte Schnittstelle zwischen Adaptern und Engine-Core. Enthält nur Interfaces, Wertetypen, Komponentenschemas und Konstanten. Keine Implementierung.

## Abhängigkeiten
Keine zu anderen Projektmodulen (Maven Enforcer). Nur JDK, keine Reflection, keine Konsolenausgabe (ArchUnit, `ArchitectureTest`).

## Konventionen
- Rechts-händig, Y oben, Meter, Radiant, Sekunden (ADR-007). Zahlen sind `float` (ADR-016).
- Transzendente Funktionen über `StrictMath`, damit Ergebnisse auf jeder Plattform bitgleich sind.
- Komponenten des Cores sind unveränderliche Records (ADR-016). `equals` ist exakt (`-0.0f != 0.0f`), für Rechenergebnisse gibt es `approxEquals` mit `MathConstants.EPSILON = 1e-5f`.

## Inhalt (Stand M1)

| Paket | Typen |
|---|---|
| `engine.api` | `Engine` (Fassade: `world`, `register`, `step`, `frameCount`, `shutdown`), `EngineException` (Basis aller Fehler, ungeprüft) |
| `engine.api.math` | `Vec2`, `Vec3`, `Quat` (Records), `Mat4` (finale Klasse, spaltenweise, Spaltenvektoren), `MathConstants` |
| `engine.api.world` | `World`, `Entity` (Index untere 32 Bit, Generation obere 32 Bit), `ComponentStore<T>`, `EntityView`, Exceptions `StaleEntityException`, `MissingComponentException`, `DuplicateComponentException`, `UnregisteredComponentException`, `HierarchyException`, `ReservedComponentException` |
| `engine.api.component` | Schemas v1: `Transform`, `WorldTransform`, `Parent`, `Children`, `Renderable`, `InputState`; `Schemas` (Namen/Versionen als `ComponentType`, `CORE_OWNED`) |
| `engine.api.schedule` | `Phase` (`EARLY`, `FIXED`, `UPDATE`, `LATE`), `Time` (Delta, Fixed-Delta, Frame ab 0), `PhaseCallback`, `FrameContext` (Phase, Zeit, Welt, Input, Event-Bus) |
| `engine.api.input` | `Input` (`axis`, `isHeld`, `wasPressed`, `wasReleased`) |
| `engine.api.render` | `Renderer extends Subsystem` (`framesRendered`) |
| `engine.api` (M4a) | `EngineApiVersion.VERSION` (geprüft gegen `[engine.provides] core-api`) |
| `engine.api.boot` (M4a) | `EngineBinding` (`adapter`, `install(Engine)`, `spawn(name, script)`): ein Adapter meldet sich per `ServiceLoader` bei `launcher-boot` an; so setzt der Launcher Core und Adapter zusammen, ohne dass einer den anderen kennt. `spawn` ist Übergang bis zum Szenenformat (ADR-010). |
| `engine.api.subsystem` | `Subsystem` (`info`, `init`, `run`, `shutdown`), `SubsystemInfo`, `ComponentType`, `EventType`, `Event`, `EventBus`, `SubsystemConfigurationException` |

## Welt (ADR-016, ACCEPTED)

```java
public interface World {
    Entity create();                                  // sofort
    void destroy(Entity e);                           // Strukturänderung, Kinder rekursiv
    boolean isAlive(Entity e);
    List<Entity> entities();                          // aufsteigend nach Index
    <T> void registerComponent(Class<T> type);        // Erweiterungspunkt, idempotent
    <T> void add(Entity e, T component);              // Strukturänderung
    <T> void set(Entity e, T component);              // ersetzt Wert, sofort
    <T> T get(Entity e, Class<T> type);
    <T> boolean has(Entity e, Class<T> type);
    <T> void remove(Entity e, Class<T> type);         // Strukturänderung
    <T> ComponentStore<T> store(Class<T> type);       // Iteration aufsteigend nach Index
    void setParent(Entity child, Entity parent);      // Strukturänderung, null löst
    Entity parentOf(Entity e);
    List<Entity> childrenOf(Entity e);                // Anhängereihenfolge
    Mat4 worldMatrix(Entity e);                       // auf Abruf über die Elternkette
    EntityView view(Entity e);
    String dump();                                    // deterministisch
}
```

- **Strukturänderungen** (`destroy`, `add`, `remove`, `setParent`) während einer Phase wirken **am Ende dieser Phase**, außerhalb eines Frames sofort. Vorbedingungen werden beim Aufruf gegen den erwarteten Zustand geprüft (aktueller Zustand plus anstehende Befehle). Befehle für bis dahin zerstörte Entities entfallen.
- `create` und `set` wirken immer sofort. Eine neu erzeugte Entity bekommt Komponenten aus derselben Phase erst am Phasenende.
- Pro Entity höchstens eine Komponente je Typ, der Typ ist die exakte Klasse. `toString()` der Komponenten muss deterministisch sein (für `dump`).
- **Vom Core verwaltet:** `Parent`, `Children` (nur über `setParent`), `WorldTransform` (zu Beginn von `LATE` berechnet). Direkte Schreibzugriffe werfen `ReservedComponentException`.
- Alle Methoden mit Entity-Parameter werfen `StaleEntityException`, wenn die Entity nicht lebt. Die Meldung unterscheidet „existiert nicht", „zerstört" und „veraltet".

## Subsystem-Vertrag (ADR-015, PROPOSED)

- `SubsystemInfo(id, kind, reads, writes, publishes, consumes, requires, conflictsWith)`. Komponenten und Ereignisse werden mit Name und Version deklariert (`ComponentType`, `EventType`, sortierbar).
- Subsysteme laufen in **jedem** Phasenschritt vor den Callbacks, in Registrierungsreihenfolge, und filtern selbst nach `context.phase()`. `init` läuft einmal beim Start außerhalb eines Frames.
- `EventBus`: Was im Phasenschritt k veröffentlicht wird, ist genau im Schritt k+1 sichtbar, auch über Frame-Grenzen. Reihenfolge = Veröffentlichungsreihenfolge (`Event.sequence`). Der Bus aus `init` prüft die deklarierten Typen, `FrameContext.events()` (für Adapter) nicht.
- Startprüfung: IDs eindeutig, höchstens ein Schreiber je Komponente (`Schemas.CORE_OWNED` gehört dem Core), `requires` passt auf ID oder Art eines *anderen* Subsystems, `conflictsWith` passt auf keines. Fehler: `SubsystemConfigurationException` mit den beteiligten IDs.

## Regeln
- Jede Änderung ist eine bewusste Entscheidung (ADR-011). Änderungen passen alle Adapter im selben Commit an.
- Keine Unity-, Godot- oder Engine-Core-Typen in der API.
- Alle Typen dokumentiert mit Javadoc inklusive Einheiten und Konventionen (Doclint ohne Warnungen).

## Tests
- Mathe-Wertetypen: Operationen, Randfälle (Nullvektor, Einheitsquaternion, Toleranz), Kreuzprodukt rechts-händig, Rotation +X um +Y ergibt −Z.
- Keine Verhaltenstests der Engine, die gehören in `engine-core`.

## Nicht-Ziele
- Keine Rendering-, Physik- oder Audio-Implementierung.
- Keine Unity-/Godot-Begriffe (`GameObject`, `Node`).
