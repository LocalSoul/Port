package io.github.localsoul.port.generator.java;

import java.util.Set;

/**
 * Konfiguration des Generators.
 *
 * @param basePackage Zielpaket; IR-Namespaces werden zu Unterpaketen in Kleinbuchstaben
 * @param mapping Mapping des Adapters (und später von {@code runtime-system})
 * @param manualClasses qualifizierte IR-Typnamen, für die {@code manual/Foo.java} existiert; für
 *     sie wird kein dünnes {@code Foo} erzeugt (unveränderlich)
 */
public record GeneratorConfig(String basePackage, Mapping mapping, Set<String> manualClasses) {

  /** Kopiert die Menge. */
  public GeneratorConfig {
    manualClasses = Set.copyOf(manualClasses);
  }
}
