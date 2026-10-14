package io.github.localsoul.port.engine.api;

/**
 * Version der Engine-API. Engine-Recomps nennen in {@code [engine.provides] core-api} den Bereich,
 * den sie brauchen; der Launcher prüft ihn ({@code docs/projects.md}). Inkompatible Änderungen der
 * API erhöhen die Version (ADR-011, OPEN).
 */
public final class EngineApiVersion {

  /** Aktuelle Version. */
  public static final String VERSION = "0.1";

  private EngineApiVersion() {}
}
