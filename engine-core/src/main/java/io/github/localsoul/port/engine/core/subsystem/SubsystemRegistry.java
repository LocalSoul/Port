package io.github.localsoul.port.engine.core.subsystem;

import io.github.localsoul.port.engine.api.component.Schemas;
import io.github.localsoul.port.engine.api.subsystem.ComponentType;
import io.github.localsoul.port.engine.api.subsystem.Subsystem;
import io.github.localsoul.port.engine.api.subsystem.SubsystemConfigurationException;
import io.github.localsoul.port.engine.api.subsystem.SubsystemInfo;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Prüft eine Liste von Subsystemen vor dem Start. Geprüft wird in Registrierungsreihenfolge, Mengen
 * werden sortiert durchlaufen, damit die erste gemeldete Verletzung deterministisch ist.
 *
 * <ol>
 *   <li>IDs sind eindeutig.
 *   <li>Jede Komponente hat höchstens einen Schreiber; {@link Schemas#CORE_OWNED} gehört dem Core.
 *   <li>Jedes {@code requires} passt auf ID oder Art eines anderen Subsystems.
 *   <li>Kein {@code conflictsWith} passt auf ID oder Art eines anderen Subsystems.
 * </ol>
 */
public final class SubsystemRegistry {

  /** Pseudo-ID des Cores als Besitzer seiner Komponenten. */
  public static final String CORE_ID = "core";

  private SubsystemRegistry() {}

  /**
   * Prüft die Kombination.
   *
   * @param subsystems Subsysteme in Registrierungsreihenfolge
   * @throws SubsystemConfigurationException bei der ersten Verletzung, mit beteiligten IDs
   */
  public static void validate(List<Subsystem> subsystems) {
    List<SubsystemInfo> infos = new ArrayList<>();
    for (Subsystem subsystem : subsystems) {
      infos.add(subsystem.info());
    }
    checkUniqueIds(infos);
    checkSingleWriter(infos);
    for (SubsystemInfo info : infos) {
      checkRequires(info, infos);
      checkConflicts(info, infos);
    }
  }

  private static void checkUniqueIds(List<SubsystemInfo> infos) {
    List<String> seen = new ArrayList<>();
    for (SubsystemInfo info : infos) {
      if (seen.contains(info.id())) {
        throw new SubsystemConfigurationException(
            "Subsystem-ID '" + info.id() + "' ist doppelt registriert");
      }
      seen.add(info.id());
    }
  }

  private static void checkSingleWriter(List<SubsystemInfo> infos) {
    Map<ComponentType, String> owners = new TreeMap<>();
    for (ComponentType coreOwned : Schemas.CORE_OWNED) {
      owners.put(coreOwned, CORE_ID);
    }
    for (SubsystemInfo info : infos) {
      for (ComponentType type : new TreeSet<>(info.writes())) {
        String owner = owners.get(type);
        if (owner != null) {
          throw new SubsystemConfigurationException(
              "Komponente "
                  + type
                  + " wird von '"
                  + owner
                  + "' und '"
                  + info.id()
                  + "' geschrieben; erlaubt ist genau ein Schreiber");
        }
        owners.put(type, info.id());
      }
    }
  }

  private static void checkRequires(SubsystemInfo info, List<SubsystemInfo> infos) {
    for (String required : new TreeSet<>(info.requires())) {
      if (findOther(info, required, infos) == null) {
        throw new SubsystemConfigurationException(
            "Subsystem '"
                + info.id()
                + "' braucht '"
                + required
                + "', aber kein anderes Subsystem hat diese ID oder Art");
      }
    }
  }

  private static void checkConflicts(SubsystemInfo info, List<SubsystemInfo> infos) {
    for (String conflict : new TreeSet<>(info.conflictsWith())) {
      SubsystemInfo other = findOther(info, conflict, infos);
      if (other != null) {
        throw new SubsystemConfigurationException(
            "Subsystem '"
                + info.id()
                + "' verträgt sich nicht mit '"
                + other.id()
                + "' (conflictsWith '"
                + conflict
                + "')");
      }
    }
  }

  /** Erstes andere Subsystem, dessen ID oder Art {@code name} ist. */
  private static SubsystemInfo findOther(
      SubsystemInfo self, String name, List<SubsystemInfo> infos) {
    for (SubsystemInfo other : infos) {
      boolean matches = other.id().equals(name) || other.kind().equals(name);
      if (other != self && matches) {
        return other;
      }
    }
    return null;
  }
}
