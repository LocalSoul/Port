package io.github.localsoul.launcher.manifest;

import io.github.localsoul.port.engine.api.EngineApiVersion;

/** Version der Engine-API der Plattform (aus {@code engine-api}, nur die Konstante). */
public final class CoreApi {

  private CoreApi() {}

  /**
   * Version der Engine-API, gegen die geprüft wird.
   *
   * @return z. B. {@code 0.1}
   */
  public static String version() {
    return EngineApiVersion.VERSION;
  }

  /**
   * Bereich für neue Engine-Projekte: diese Version bis zur nächsten Nebenversion.
   *
   * @return z. B. {@code >=0.1 <0.2}
   */
  public static String defaultRange() {
    String[] parts = version().split("\\.");
    long minor = parts.length > 1 ? Long.parseLong(parts[1]) : 0;
    return ">=" + version() + " <" + parts[0] + "." + (minor + 1);
  }
}
