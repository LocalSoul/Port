package io.github.localsoul.port.engine.api.subsystem;

/**
 * Name und Version eines Ereignistyps. Sortierung nach Name, dann Version.
 *
 * @param name Name, z. B. {@code collision.enter}
 * @param version Version des Nutzdatenformats, ab 1
 */
public record EventType(String name, int version) implements Comparable<EventType> {

  @Override
  public int compareTo(EventType other) {
    int result = name.compareTo(other.name);
    return result != 0 ? result : Integer.compare(version, other.version);
  }

  /** Liefert {@code name@version}. */
  @Override
  public String toString() {
    return name + "@" + version;
  }
}
