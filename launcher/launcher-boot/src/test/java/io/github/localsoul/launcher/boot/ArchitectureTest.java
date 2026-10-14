package io.github.localsoul.launcher.boot;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** launcher-boot kennt keinen Adapter und kein Spiel; Reflection nur in {@link BootMain}. */
class ArchitectureTest {

  private static JavaClasses classes;

  @BeforeAll
  static void importClasses() {
    classes =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.launcher.boot");
  }

  @Test
  void noAdapterOrGameCode() {
    noClasses()
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.adapter..", "game..", "io.github.localsoul.port.cli..")
        .check(classes);
  }

  @Test
  void reflectionOnlyInBootMain() {
    noClasses()
        .that()
        .doNotHaveFullyQualifiedName(BootMain.class.getName())
        .should()
        .dependOnClassesThat()
        .resideInAnyPackage("java.lang.reflect..")
        .check(classes);
  }
}
