package io.github.localsoul.port.lint;

import io.github.localsoul.port.ir.validate.Diagnostic;
import io.github.localsoul.port.ir.validate.IrValidator;
import java.util.List;

/**
 * IR001–IR005 aus dem IR-Validator. Externe Typen gelten dem Validator als bekannt: ob Adapter und
 * {@code runtime-system} sie bereitstellen, prüft SH001.
 */
final class ValidatorRule implements Rule {

  @Override
  public void check(LintInput input, ProjectModel model, List<LintFinding> findings) {
    IrValidator validator = new IrValidator(name -> true);
    for (IrSource source : input.sources()) {
      if (source.error() != null) {
        add(IrValidator.syntaxError(source.error()), findings);
        continue;
      }
      for (Diagnostic diagnostic : validator.validate(source.file())) {
        add(diagnostic, findings);
      }
    }
  }

  private static void add(Diagnostic diagnostic, List<LintFinding> findings) {
    findings.add(
        new LintFinding(
            diagnostic.id(),
            Severity.fromSpelling(diagnostic.severity().spelling()),
            diagnostic.symbol(),
            diagnostic.message(),
            diagnostic.location() == null ? null : diagnostic.location().toString(),
            diagnostic.hint()));
  }
}
