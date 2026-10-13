package io.github.localsoul.runtime.system;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** {@code runtime-system} hängt nur vom JDK ab (AGENTS.md, Regel 1). */
class ArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.runtime.system");
  }

  @Test
  void dependsOnlyOnJdk() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage("io.github.localsoul.runtime.system..", "java..")
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
        .check(productionClasses);
  }
}
