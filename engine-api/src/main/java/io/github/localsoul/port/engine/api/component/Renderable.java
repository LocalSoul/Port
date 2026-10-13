package io.github.localsoul.port.engine.api.component;

/**
 * Platzhalter-Schema für darstellbare Entities (ADR-014 ist offen). Gezeichnet wird an der Stelle
 * von {@link WorldTransform}.
 *
 * @param resource ID der darzustellenden Ressource (Format folgt mit ADR-010)
 * @param visible sichtbar; Default {@code true}
 */
public record Renderable(String resource, boolean visible) {

  /** Schemaversion. */
  public static final int SCHEMA_VERSION = 1;

  /**
   * Sichtbares Renderable mit Default-Werten.
   *
   * @param resource ID der Ressource
   * @return Renderable mit {@code visible = true}
   */
  public static Renderable of(String resource) {
    return new Renderable(resource, true);
  }
}
