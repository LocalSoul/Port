package io.github.localsoul.port.index;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** index nutzt nur ir und das JDK (inkl. java.sql), keinen Treiber direkt, keine Konsole. */
class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.port.index");
  }

  @Test
  void dependsOnlyOnIrAndJdk() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.port.index..", "io.github.localsoul.port.ir..", "java..")
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
        .check(classes);
  }
}
