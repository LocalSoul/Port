package io.github.localsoul.port.engine.api.subsystem;

/**
 * Name und Schemaversion eines Komponententyps, wie er in Subsystem-Deklarationen steht. Sortierung
 * nach Name, dann Version.
 *
 * @param name Name des Schemas, z. B. {@code Transform}
 * @param version Schemaversion, ab 1
 */
public record ComponentType(String name, int version) implements Comparable<ComponentType> {

  @Override
  public int compareTo(ComponentType other) {
    int result = name.compareTo(other.name);
    return result != 0 ? result : Integer.compare(version, other.version);
  }

  /** Liefert {@code name@version}, z. B. {@code Transform@1}. */
  @Override
  public String toString() {
    return name + "@" + version;
  }
}
