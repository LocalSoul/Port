package io.github.localsoul.port.generator.java;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Der Generator hängt nur von {@code ir} ab, nie von Adaptern oder der Engine (AGENTS.md, Regel 1).
 */
class ArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.port.generator.java");
  }

  @Test
  void dependsOnlyOnIrAndJdk() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.port.generator.java..", "io.github.localsoul.port.ir..", "java..")
        .check(productionClasses);
  }

  @Test
  void neverUsesAdaptersOrEngine() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("..adapter..", "..engine..")
        .orShould()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .orShould()
        .accessField(System.class, "out")
        .check(productionClasses);
  }
}
