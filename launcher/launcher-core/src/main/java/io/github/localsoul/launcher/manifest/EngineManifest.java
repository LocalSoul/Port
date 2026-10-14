package io.github.localsoul.launcher.manifest;

import java.util.List;

/**
 * {@code port.toml} einer Engine-Recomp ({@code docs/projects.md}). {@code [engine.build]} ist eine
 * Ergänzung aus M4a: das Maven-Modul mit Adapter, Mapping und Klassen. Gestartet wird über {@code
 * launcher-boot}, der Adapter meldet sich per {@code EngineBinding}. {@code [engine.sources]} und
 * {@code [conformance]} kommen später.
 *
 * @param name Projektname
 * @param adapter Name der bereitgestellten API, z. B. {@code unity}
 * @param api Version der nachgebildeten API, z. B. {@code 2022.3}
 * @param coreApi benötigter Bereich der Engine-API ({@link VersionRange})
 * @param providers bereitgestellte Subsystem-Provider (ab M12)
 * @param module Maven-Modul relativ zum Engine-Ordner, z. B. {@code adapter-unity}
 */
public record EngineManifest(
    String name, String adapter, String api, String coreApi, List<String> providers, String module)
    implements ProjectManifest {

  /** Kopiert die Liste. */
  public EngineManifest {
    providers = List.copyOf(providers);
  }

  @Override
  public String kind() {
    return "engine";
  }

  @Override
  public String toToml() {
    TomlText out = new TomlText();
    out.table("project").entry("kind", kind()).entry("name", name);
    out.table("engine.provides")
        .entry("adapter", adapter)
        .entry("api", api)
        .entry("core-api", coreApi)
        .entry("providers", providers);
    out.table("engine.build").entry("module", module);
    return out.text();
  }
}
