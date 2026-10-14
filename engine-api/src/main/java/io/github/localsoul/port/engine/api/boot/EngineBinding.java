package io.github.localsoul.port.engine.api.boot;

import io.github.localsoul.port.engine.api.Engine;

/**
 * Anbindung einer Engine-Recomp (Adapter) an eine laufende Engine, gefunden per {@link
 * java.util.ServiceLoader} in der Start-JVM ({@code launcher-boot}). So setzt der Launcher Core und
 * Adapter zusammen, ohne dass der Adapter den Core oder der Core den Adapter kennt (AGENTS.md,
 * Regeln 1 und 12).
 *
 * <p>Übergang bis zum Szenenformat (ADR-010): {@link #spawn} legt je Skript ein Objekt an.
 */
public interface EngineBinding {

  /**
   * Name der bereitgestellten API, wie {@code [engine.provides] adapter} im Manifest der Engine.
   *
   * @return z. B. {@code unity}
   */
  String adapter();

  /**
   * Bindet den Adapter an die Engine (Komponenten, Callbacks).
   *
   * @param engine Engine vor dem ersten Frame
   */
  void install(Engine engine);

  /**
   * Legt beim Start ein Objekt mit einem Skript des Spiels an.
   *
   * @param name Name des Objekts
   * @param script Instanz einer generierten Spielklasse
   * @throws IllegalArgumentException wenn der Adapter mit dem Skript nichts anfangen kann
   */
  void spawn(String name, Object script);
}
