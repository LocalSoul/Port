package io.github.localsoul.port.ir;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchRule;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/** Paketregeln für das Modul {@code ir} (AGENTS.md, Regel 1; {@code docs/conventions.md}). */
class ArchitectureTest {

  private static JavaClasses productionClasses;

  @BeforeAll
  static void importClasses() {
    productionClasses =
        new ClassFileImporter()
            .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
            .importPackages("io.github.localsoul.port.ir");
  }

  @Test
  void irDependsOnlyOnJdkAndItself() {
    ArchRule rule =
        classes()
            .should()
            .onlyDependOnClassesThat()
            .resideInAnyPackage("io.github.localsoul.port.ir..", "java..");
    rule.check(productionClasses);
  }

  @Test
  void irUsesNoReflection() {
    ArchRule rule =
        noClasses().should().dependOnClassesThat().resideInAnyPackage("java.lang.reflect..");
    rule.check(productionClasses);
  }

  @Test
  void libraryCodeDoesNotWriteToConsole() {
    ArchRule rule =
        noClasses()
            .should()
            .accessField(System.class, "out")
            .orShould()
            .accessField(System.class, "err");
    rule.check(productionClasses);
  }

  @Test
  void lowerLayersDoNotDependOnUpperLayers() {
    ArchRule sexpr =
        noClasses()
            .that()
            .resideInAnyPackage("..ir.sexpr..", "..ir.atom..", "..ir.ast..")
            .should()
            .dependOnClassesThat()
            .resideInAnyPackage(
                "..ir.reader..", "..ir.printer..", "..ir.validate..", "..ir.refs..");
    sexpr.check(productionClasses);
  }
}
