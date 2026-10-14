package io.github.localsoul.port.lint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.reader.IrReader;
import io.github.localsoul.port.ir.refs.SymbolSource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Je Regel Treffer und Nicht-Treffer an kleinen IR-Ausschnitten. */
class LinterTest {

  private static final String HEADER = "(ir :version 1 :assembly \"T\")\n";

  static IrSource source(String path, String body) {
    String text = HEADER + body;
    try {
      return new IrSource(path, text, new IrReader().read(text, path), null);
    } catch (IrSyntaxException e) {
      return new IrSource(path, text, null, e);
    }
  }

  private static List<String> run(
      List<IrSource> sources,
      List<ManualOverride> overrides,
      Coverage coverage,
      LintConfig config) {
    List<String> result = new ArrayList<>();
    for (LintFinding finding : Linter.lint(new LintInput(sources, overrides, coverage), config)) {
      result.add(finding.id() + " " + finding.severity().spelling() + " " + finding.symbol());
    }
    return result;
  }

  private static List<String> run(String body, Coverage coverage) {
    return run(List.of(source("ir/t.ir", body)), List.of(), coverage, LintConfig.DEFAULT);
  }

  private static List<String> only(String prefix, List<String> findings) {
    List<String> result = new ArrayList<>();
    for (String finding : findings) {
      if (finding.startsWith(prefix)) {
        result.add(finding);
      }
    }
    return result;
  }

  private static Coverage coverage(String... full) {
    Coverage coverage = new Coverage();
    for (String symbol : full) {
      coverage.put(symbol, Coverage.FULL);
    }
    return coverage;
  }

  @Test
  void ir001AndIr003FromTheValidator() {
    List<String> syntax =
        run(List.of(source("ir/b.ir", "(class A")), List.of(), coverage(), LintConfig.DEFAULT);
    assertEquals(List.of("IR001 error null"), syntax);
    assertTrue(run("(class A (field x unknown))", coverage()).contains("IR003 error A::x"));
  }

  @Test
  void pt001ReflectionOutsideManual() {
    String body =
        "(class A (method M void ((t System.Type)) (block (expr (call t System.Type::GetMethod#string \"x\"))))"
            + " (method N void ((t System.Type)) (block (expr (call t System.Type::GetMethod#string \"x\")))"
            + " :manual \"Reflection\"))";
    assertEquals(
        List.of("PT001 error A::M#System.Type"), only("PT001", run(body, coverage("System.Type"))));
    assertTrue(PortabilityRule.isReflection("System.Reflection.MethodInfo"));
    assertTrue(PortabilityRule.isReflection("System.Activator::CreateInstance#System.Type"));
  }

  @Test
  void pt002PointerType() {
    assertEquals(
        List.of("PT002 error A::p"), only("PT002", run("(class A (field p ptr))", coverage())));
  }

  @Test
  void pt003YieldInTryWithFinallyOrLambda() {
    String body =
        "(class A (method M System.Collections.IEnumerator () (block"
            + " (yield-return 1i32)"
            + " (try (block (yield-return 2i32)) (finally (block)))"
            + " (expr (lambda () (block (yield-break)))))))";
    assertEquals(
        List.of("PT003 error A::M#", "PT003 error A::M#"),
        only("PT003", run(body, coverage("System.Collections.IEnumerator"))));
  }

  @Test
  void sm001CopiesOfOwnStructs() {
    String body =
        "(struct S (field v i32) (ctor () (block)))"
            + "(class A (field f S)"
            + " (method M S ((p S)) (block"
            + " (local a S p)"
            + " (local b S (new S::.ctor#))"
            + " (local c S (copy p))"
            + " (expr (set a (field this A::f)))"
            + " (return a))))";
    assertEquals(
        List.of("SM001 warn A::M#S", "SM001 warn A::M#S", "SM001 warn A::M#S"),
        only("SM001", run(body, coverage())));
  }

  @Test
  void sh001AndSh002AgainstCoverage() {
    Coverage coverage = coverage("Ext.Api", "Ext.Api::Fine#");
    coverage.put("Ext.Api::Stubbed#", Coverage.STUB);
    String body =
        "(class A (method M void () (block"
            + " (expr (scall Ext.Api::Fine#)) (expr (scall Ext.Api::Stubbed#))"
            + " (expr (scall Ext.Api::Missing#)) (expr (scall Ext.Api::Missing#))"
            + " (local x Ext.Other))))";
    assertEquals(
        List.of("SH001 error A::M#", "SH001 error A::M#", "SH002 warn A::M#"),
        only("SH", run(body, coverage)));
  }

  @Test
  void manualOverrides() {
    String body =
        "(class A"
            + " (method M void () (block (raw cs \"x\")) :manual \"Handarbeit\")"
            + " (method N void () (block) :manual \"Handarbeit\")"
            + " (method O void () (block)))";
    IrSource source = source("ir/a.ir", body);
    String text = source.text();
    String hashM = hash(source, "(method M");
    String hashO = hash(source, "(method O");
    List<ManualOverride> overrides =
        ManualOverride.parse(
            "manual/A.java",
            "class A {\n  // port:override A::M# ir="
                + hashM.substring(0, 12)
                + "\n"
                + "  // port:override A::O# ir="
                + hashO.substring(0, 16)
                + "\n"
                + "  // port:override A::Weg# ir=0123456789ab\n"
                + "  //port:override A::N# ir=ffffffffffff\n}\n");
    assertEquals(4, overrides.size());
    assertEquals("manual/A.java:2:3", overrides.get(0).location());
    List<String> findings = run(List.of(source), overrides, coverage(), LintConfig.DEFAULT);
    assertEquals(
        List.of("MN004 info A::O#", "MN001 error A::Weg#", "MN002 error A::N#"),
        only("MN", findings));
    List<String> withoutOverrides = run(List.of(source), List.of(), coverage(), LintConfig.DEFAULT);
    assertEquals(List.of("MN003 warn A::M#", "MN003 warn A::N#"), only("MN", withoutOverrides));
    assertTrue(text.contains("(method M"));
  }

  private static String hash(IrSource source, String start) {
    String[] lines = source.text().split("\n", -1);
    for (int i = 0; i < lines.length; i++) {
      int column = lines[i].indexOf(start);
      if (column >= 0) {
        return SymbolSource.hash(source.text(), i + 1, column + 1);
      }
    }
    throw new AssertionError(start);
  }

  @Test
  void configDisablesRulesAndChangesSeverity() {
    String body = "(class A (field p ptr) (method M void () (block (expr (scall Ext::Gone#)))))";
    LintConfig config = new LintConfig(Set.of("SH001"), Map.of("PT002", Severity.WARN));
    List<String> findings = run(List.of(source("ir/t.ir", body)), List.of(), coverage(), config);
    assertEquals(List.of("PT002 warn A::p"), findings);
  }
}
