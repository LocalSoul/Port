package io.github.localsoul.launcher;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Der Launcher kennt weder Spiel noch Adapter noch Engine-Core (docs/modules/launcher.md); aus
 * {@code engine-api} nur Metadaten.
 */
class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.launcher");
  }

  @Test
  void noGameAdapterOrCore() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.adapter..",
            "io.github.localsoul.port.engine.core..",
            "io.github.localsoul.launcher.boot..",
            "io.github.localsoul.port.cli..",
            "game..")
        .check(classes);
  }

  @Test
  void noReflectionOrConsole() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .orShould()
        .accessField(System.class, "out")
        .orShould()
        .accessField(System.class, "err")
        .check(classes);
  }
}
