package io.github.localsoul.port.engine.core;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.library.dependencies.SlicesRuleDefinition.slices;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Abhängigkeitsregeln für {@code engine-core} (AGENTS.md, Regeln 1 und 10). */
class ArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.port.engine.core");
  }

  @Test
  void dependsOnlyOnAllowedPackages() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.port.engine.core..",
            "io.github.localsoul.port.engine.api..",
            "java..")
        .check(productionClasses);
  }

  @Test
  void usesNoReflection() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .check(productionClasses);
  }

  @Test
  void doesNotWriteToConsole() {
    noClasses()
        .should()
        .accessField(System.class, "out")
        .orShould()
        .accessField(System.class, "err")
        .check(productionClasses);
  }

  @Test
  void coreDoesNotDependOnAdaptersFrontendsOrIr() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapter..", "..frontend..", "io.github.localsoul.port.ir..")
        .check(productionClasses);
  }

  @Test
  void subsystemImplementationsDoNotDependOnEachOther() {
    slices()
        .matching("io.github.localsoul.port.engine.core.subsystems.(*)..")
        .should()
        .notDependOnEachOther()
        .check(productionClasses);
  }

  @Test
  void subsystemImplementationsUseOnlyTheEngineApi() {
    classes()
        .that()
        .resideInAPackage("io.github.localsoul.port.engine.core.subsystems..")
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.port.engine.core.subsystems..",
            "io.github.localsoul.port.engine.api..",
            "java..")
        .check(productionClasses);
  }
}
