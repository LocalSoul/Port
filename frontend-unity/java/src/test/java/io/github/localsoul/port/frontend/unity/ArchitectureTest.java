package io.github.localsoul.port.frontend.unity;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Abhängigkeitsregeln für {@code frontend-unity} (AGENTS.md, Regel 1). */
class ArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.port.frontend.unity");
  }

  @Test
  void dependsOnlyOnIrAndJdk() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.port.frontend.unity..", "io.github.localsoul.port.ir..", "java..")
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
}
