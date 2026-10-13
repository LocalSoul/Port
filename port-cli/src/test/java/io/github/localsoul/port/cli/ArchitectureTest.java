package io.github.localsoul.port.cli;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Regeln aus {@code docs/modules/port-cli.md} und AGENTS.md, Regel 1. */
class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.port");
  }

  @Test
  void noAdapterEngineOrRuntimeCode() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.adapter..",
            "io.github.localsoul.port.engine..",
            "io.github.localsoul.runtime..")
        .check(classes);
  }

  @Test
  void noReflection() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .check(classes);
  }

  @Test
  void coreAndOutputDoNotKnowTheCommandLine() {
    noClasses()
        .that()
        .resideInAnyPackage(
            "io.github.localsoul.port.core..",
            "io.github.localsoul.port.output..",
            "io.github.localsoul.port.json..")
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("picocli..", "io.github.localsoul.port.cli..")
        .check(classes);
  }

  @Test
  void onlyMainUsesTheConsole() {
    noClasses()
        .that()
        .doNotHaveFullyQualifiedName("io.github.localsoul.port.cli.Main")
        .should()
        .accessField(System.class, "out")
        .orShould()
        .accessField(System.class, "err")
        .check(classes);
  }
}
