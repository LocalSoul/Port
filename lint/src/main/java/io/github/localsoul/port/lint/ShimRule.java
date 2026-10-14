package io.github.localsoul.port.lint;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.atom.ArrayType;
import io.github.localsoul.port.ir.atom.ByRefType;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.NullableType;
import io.github.localsoul.port.ir.atom.PrimitiveType;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.refs.NodeWalker;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * SH001 (genutzte API fehlt in Adapter und {@code runtime-system}) und SH002 (nur Stub) für externe
 * Typen und Member. Ein Befund je Quelle und Ziel. {@code System.Array::Length} erzeugt der
 * Generator direkt ({@code array.length}).
 */
final class ShimRule implements Rule {

  @Override
  public void check(LintInput input, ProjectModel model, List<LintFinding> findings) {
    Set<String> reported = new TreeSet<>();
    for (IrSource source : input.sources()) {
      if (source.file() != null) {
        new Walker(input.coverage(), model, reported, findings).walk(source.file());
      }
    }
  }

  private static final class Walker extends NodeWalker {

    private final Coverage coverage;
    private final ProjectModel model;
    private final Set<String> reported;
    private final List<LintFinding> findings;

    Walker(
        Coverage coverage, ProjectModel model, Set<String> reported, List<LintFinding> findings) {
      this.coverage = coverage;
      this.model = model;
      this.reported = reported;
      this.findings = findings;
    }

    @Override
    protected void onType(TypeRef type, SourceSpan span) {
      switch (type) {
        case PrimitiveType p -> {}
        case ArrayType a -> onType(a.elementType(), span);
        case NullableType n -> onType(n.underlyingType(), span);
        case ByRefType b -> onType(b.referencedType(), span);
        case NamedType n -> {
          String name = n.qualifiedName();
          if (!genericScope().contains(name) && !name.equals("unknown") && !model.isOwn(name)) {
            check(name, span);
          }
          for (TypeRef argument : n.typeArguments()) {
            onType(argument, span);
          }
        }
      }
    }

    @Override
    protected void onMember(MemberRef ref, SourceSpan span) {
      String declaring = ref.declaringType();
      if (model.isOwn(declaring)
          || declaring.equals("unknown")
          || genericScope().contains(declaring)) {
        return;
      }
      String id = ref.symbolId();
      if (id.equals("System.Array::Length")) {
        return;
      }
      check(id, span);
    }

    private void check(String target, SourceSpan span) {
      String status = coverage.status(target);
      if (Coverage.FULL.equals(status) || !reported.add(symbol() + "\u0000" + target)) {
        return;
      }
      if (status == null) {
        findings.add(
            new LintFinding(
                "SH001",
                Severity.ERROR,
                symbol(),
                "genutzte API fehlt im Adapter: " + target,
                span.toString(),
                "API im Adapter (mapping.tsv) ergänzen oder Stelle mit :manual markieren"));
      } else {
        findings.add(
            new LintFinding(
                "SH002",
                Severity.WARN,
                symbol(),
                "API nur als Stub vorhanden: " + target,
                span.toString(),
                "Verhalten im Adapter vervollständigen"));
      }
    }
  }
}
