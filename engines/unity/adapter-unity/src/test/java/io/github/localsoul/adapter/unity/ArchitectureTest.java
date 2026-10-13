package io.github.localsoul.adapter.unity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Abhängigkeitsregeln für {@code adapter-unity} (AGENTS.md, Regeln 1 und 13). */
class ArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.adapter.unity");
  }

  @Test
  void dependsOnlyOnEngineApiAndJdk() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.adapter.unity..",
            "io.github.localsoul.port.engine.api..",
            "java..")
        .check(productionClasses);
  }

  @Test
  void neverUsesEngineCore() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("io.github.localsoul.port.engine.core..")
        .check(productionClasses);
  }

  @Test
  void usesNoReflectionOrConsole() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .orShould()
        .accessField(System.class, "out")
        .orShould()
        .accessField(System.class, "err")
        .check(productionClasses);
  }
}
