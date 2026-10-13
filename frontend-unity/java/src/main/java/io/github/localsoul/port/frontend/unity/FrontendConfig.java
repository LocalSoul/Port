package io.github.localsoul.port.frontend.unity;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;

/**
 * Wie das .NET-Programm gestartet wird.
 *
 * @param commandPrefix Programm (und ggf. {@code dotnet}) vor den Argumenten (unveränderlich)
 * @param timeout maximale Laufzeit je Aufruf
 */
public record FrontendConfig(List<String> commandPrefix, Duration timeout) {

  /** Name der Systemproperty mit dem Programmpfad. */
  public static final String PROPERTY = "port.frontend.unity";

  /** Name der Umgebungsvariable mit dem Programmpfad. */
  public static final String ENVIRONMENT = "PORT_FRONTEND_UNITY";

  /** Programmname im {@code PATH}, wenn nichts konfiguriert ist. */
  public static final String DEFAULT_PROGRAM = "port-frontend-unity";

  /** Standard-Zeitlimit. */
  public static final Duration DEFAULT_TIMEOUT = Duration.ofMinutes(5);

  /** Kopiert den Befehl. */
  public FrontendConfig {
    commandPrefix = List.copyOf(commandPrefix);
  }

  /**
   * Konfiguration aus Systemproperty {@value #PROPERTY}, sonst Umgebungsvariable {@value
   * #ENVIRONMENT}, sonst {@value #DEFAULT_PROGRAM} im {@code PATH}.
   *
   * @return Konfiguration
   */
  public static FrontendConfig fromEnvironment() {
    String configured = System.getProperty(PROPERTY);
    if (configured == null || configured.isBlank()) {
      configured = System.getenv(ENVIRONMENT);
    }
    if (configured == null || configured.isBlank()) {
      configured = DEFAULT_PROGRAM;
    }
    return forProgram(configured);
  }

  /**
   * Konfiguration für einen Programmpfad. Endet er auf {@code .dll}, wird er mit {@code dotnet}
   * gestartet.
   *
   * @param program Pfad oder Name des Programms
   * @return Konfiguration mit Standard-Zeitlimit
   */
  public static FrontendConfig forProgram(String program) {
    if (program.endsWith(".dll")) {
      return new FrontendConfig(List.of("dotnet", program), DEFAULT_TIMEOUT);
    }
    return new FrontendConfig(List.of(program), DEFAULT_TIMEOUT);
  }

  /**
   * Konfiguration für eine gebaute {@code port-frontend-unity.dll}.
   *
   * @param dll Pfad zur DLL
   * @return Konfiguration
   */
  public static FrontendConfig forDll(Path dll) {
    return forProgram(dll.toString().endsWith(".dll") ? dll.toString() : dll + ".dll");
  }
}
