package io.github.localsoul.port.engine.api.component;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Eingabezustand eines Frames, geschrieben vom Input-Subsystem in {@code EARLY}. Alle Sammlungen
 * sind sortiert und unveränderlich, damit {@code toString()} (und damit der Welt-Dump)
 * deterministisch ist.
 *
 * @param axes Achsenwerte; fehlende Achsen gelten als 0 (Default)
 * @param held gehaltene Tasten; Default leer
 * @param pressed in diesem Frame gedrückte Tasten; Default leer
 * @param released in diesem Frame losgelassene Tasten; Default leer
 */
public record InputState(
    SortedMap<String, Float> axes,
    SortedSet<String> held,
    SortedSet<String> pressed,
    SortedSet<String> released) {

  /** Schemaversion. */
  public static final int SCHEMA_VERSION = 1;

  /** Keine Eingabe. */
  public static final InputState EMPTY =
      new InputState(new TreeMap<>(), new TreeSet<>(), new TreeSet<>(), new TreeSet<>());

  /** Kopiert alle Sammlungen in sortierte, unveränderliche Kopien. */
  public InputState {
    axes = Collections.unmodifiableSortedMap(new TreeMap<>(axes));
    held = sortedCopy(held);
    pressed = sortedCopy(pressed);
    released = sortedCopy(released);
  }

  /**
   * Erzeugt einen Zustand aus beliebigen Sammlungen (werden sortiert kopiert).
   *
   * @param axes Achsenwerte
   * @param held gehaltene Tasten
   * @param pressed gedrückte Tasten
   * @param released losgelassene Tasten
   * @return Zustand
   */
  public static InputState of(
      Map<String, Float> axes, Set<String> held, Set<String> pressed, Set<String> released) {
    return new InputState(
        new TreeMap<>(axes), new TreeSet<>(held), new TreeSet<>(pressed), new TreeSet<>(released));
  }

  private static SortedSet<String> sortedCopy(Set<String> values) {
    return Collections.unmodifiableSortedSet(new TreeSet<>(values));
  }
}
