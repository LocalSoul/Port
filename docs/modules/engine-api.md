# Modul `engine-api`

## Zweck
Schmale, versionierte Schnittstelle zwischen Adaptern und Engine-Core. Enthält nur Interfaces, Wertetypen und Konstanten. Keine Implementierung.

## Abhängigkeiten
Keine zu anderen Projektmodulen.

## Inhalt (Skizze, Details nach ADR-006)
- **Mathe-Wertetypen:** `Vec2`, `Vec3`, `Quat`, `Mat4` (unveränderliche Records, rechts-händig Y oben nach ADR-007).
- **Welt/Entity:** `World`, `Entity` (Form hängt an ADR-006, hinter Interface halten).
- **Komponenten:** Basistyp `Component`, Registrierung eigener Komponententypen (Erweiterungspunkt).
- **Scheduler:** `Phase` (`EARLY`, `FIXED`, `UPDATE`, `LATE`), Registrierung von Callbacks pro Phase, `Time` (Delta, Fixed-Delta, Frame).
- **Subsysteme:** `Input`, `Renderer` (zunächst nur Null-Variante, ADR-014), `Physics` und `Audio` als später gefüllte Interfaces.
- **Engine:** `Engine`-Fassade zum Erzeugen von Welt, Starten, Frame-Schritt (`step(n)`), Beenden.

## Welt und Komponenten (ADR-016, PROPOSED)

```java
public interface World {
    Entity create();
    void destroy(Entity e);                       // wirkt am Phasenende (Command-Buffer)
    boolean isAlive(Entity e);
    <T> void add(Entity e, T component);          // Typ muss registriert sein
    <T> T get(Entity e, Class<T> type);           // wirft bei fehlender Komponente
    <T> boolean has(Entity e, Class<T> type);
    <T> void remove(Entity e, Class<T> type);
    <T> ComponentStore<T> store(Class<T> type);
    <T> void registerComponent(Class<T> type);    // Erweiterungspunkt für Adapter
    String dump();                                // deterministisch
}

public interface ComponentStore<T> {
    int size();
    Entity entityAt(int denseIndex);
    T componentAt(int denseIndex);                // Iteration per for-Schleife
}

public record Entity(long id) {                   // Index und Generation in einer ID
    int index();
    int generation();
}
```

Hinweis: Die Signaturen sind eine Skizze. Entscheidend sind Entity-Handles mit Generation, getrennter Speicher je Komponententyp, explizite Iteration und der Command-Buffer. Details dürfen im Code präzisiert werden, Abweichungen vom Entwurf gehören in ADR-016.

## Subsystem-Vertrag (ADR-015, PROPOSED)

Subsysteme (`Input`, `Renderer`, `Physics`, `Audio`) sind **getrennte Interfaces**. Sie tauschen Daten nur über den Core aus, nie direkt.

```java
public interface Subsystem {
    SubsystemInfo info();                  // Deklaration, siehe unten
    void init(World world, EventBus bus);
    void run(Phase phase, FrameContext ctx);
    void shutdown();
}

public record SubsystemInfo(
    String id,                             // z. B. "input-godot-like"
    String kind,                           // "input", "render", "physics", "audio", ...
    Set<ComponentType> reads,
    Set<ComponentType> writes,             // Besitzer: genau ein Subsystem je Komponente
    Set<EventType> publishes,
    Set<EventType> consumes,
    Set<String> requires,                  // Pflichtpaare (andere Subsystem-Arten oder IDs)
    Set<String> conflictsWith) {}
```

- `EventBus`: Ereignisse mit deterministischer Reihenfolge. Semantik (sofort oder pro Phase gepuffert) ist eine offene Teilfrage in ADR-015, bis dahin gepuffert pro Phase und dokumentiert.
- **Komponentenschemas** (`Transform`, `Renderable`, `Collider`, `AudioSource`, ...) sind engine-neutrale, versionierte Datentypen in dieser API. Felder, die nicht jeder Provider liefert, haben dokumentierte Defaults.
- Beim Start prüft die Engine, dass pro Komponente höchstens ein Subsystem in `writes` steht und dass alle `requires` erfüllt sind. Fehler sind eindeutige Exceptions mit Angabe der beteiligten Provider.

## Regeln
- Jede Änderung ist eine bewusste Entscheidung (ADR-011). Änderungen passen alle Adapter im selben Commit an.
- Keine Unity-, Godot- oder Engine-Core-Typen in der API.
- Alle Typen dokumentiert mit Javadoc inklusive Einheiten und Konventionen.

## Tests
- Mathe-Wertetypen: Gleichheit, Operationen, Randfälle (Null-Vektoren, Normalisierung).
- Keine Verhaltenstests der Engine, die gehören in `engine-core`.

## Nicht-Ziele
- Keine Rendering-, Physik- oder Audio-Implementierung.
- Keine Unity-/Godot-Begriffe (`GameObject`, `Node`).
