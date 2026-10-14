package io.github.localsoul.port.render.swing;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;

import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.Test;

/** Das Backend kennt nur engine-api und das JDK (ADR-014, Regel 10). */
class ArchitectureTest {

  @Test
  void dependsOnlyOnEngineApiAndJdk() {
    classes()
        .should()
        .onlyDependOnClassesThat()
        .resideInAnyPackage(
            "io.github.localsoul.port.render.swing..",
            "io.github.localsoul.port.engine.api..",
            "java..",
            "javax..")
        .check(
            new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("io.github.localsoul.port.render.swing"));
  }
}
