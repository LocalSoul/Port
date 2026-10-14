package io.github.localsoul.port.lint;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.YieldBreakStmt;
import io.github.localsoul.port.ir.ast.YieldReturnStmt;
import io.github.localsoul.port.ir.atom.ArrayType;
import io.github.localsoul.port.ir.atom.ByRefType;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.NullableType;
import io.github.localsoul.port.ir.atom.Primitive;
import io.github.localsoul.port.ir.atom.PrimitiveType;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.refs.NodeWalker;
import java.util.List;

/**
 * PT001 Reflection, PT002 Zeiger ({@code ptr}), PT003 {@code yield} in {@code try} mit {@code
 * finally} oder in einem Lambda. Stellen in Handarbeitsstellen ({@code :manual}, {@code raw}) sind
 * ausgenommen: Sie meldet MN003.
 */
final class PortabilityRule implements Rule {

  private static final List<String> REFLECTION_MEMBERS =
      List.of(
          "System.Type::GetMethod",
          "System.Type::GetMethods",
          "System.Type::GetField",
          "System.Type::GetFields",
          "System.Type::GetProperty",
          "System.Type::GetProperties",
          "System.Type::GetMember",
          "System.Type::GetMembers",
          "System.Type::GetType",
          "System.Type::InvokeMember",
          "System.Activator::CreateInstance");

  @Override
  public void check(LintInput input, ProjectModel model, List<LintFinding> findings) {
    for (IrSource source : input.sources()) {
      if (source.file() != null) {
        new Walker(model, findings).walk(source.file());
      }
    }
  }

  /** {@code true} für Namen aus {@code System.Reflection} und die bekannten Reflection-Methoden. */
  static boolean isReflection(String target) {
    if (target.startsWith("System.Reflection.")) {
      return true;
    }
    int hash = target.indexOf('#');
    String withoutParameters = hash < 0 ? target : target.substring(0, hash);
    int generic = withoutParameters.indexOf('<', withoutParameters.indexOf("::") + 2);
    if (generic > 0) {
      withoutParameters = withoutParameters.substring(0, generic);
    }
    return REFLECTION_MEMBERS.contains(withoutParameters);
  }

  private static boolean containsPointer(TypeRef type) {
    return switch (type) {
      case PrimitiveType p -> p.primitive() == Primitive.PTR;
      case ArrayType a -> containsPointer(a.elementType());
      case NullableType n -> containsPointer(n.underlyingType());
      case ByRefType b -> containsPointer(b.referencedType());
      case NamedType n -> {
        boolean found = false;
        for (TypeRef argument : n.typeArguments()) {
          found = found || containsPointer(argument);
        }
        yield found;
      }
    };
  }

  private static final class Walker extends NodeWalker {

    private final ProjectModel model;
    private final List<LintFinding> findings;

    Walker(ProjectModel model, List<LintFinding> findings) {
      this.model = model;
      this.findings = findings;
    }

    private boolean exempt() {
      return model.isManual(symbol());
    }

    @Override
    protected void onType(TypeRef type, SourceSpan span) {
      if (exempt()) {
        return;
      }
      if (type instanceof NamedType named && isReflection(named.qualifiedName())) {
        reflection(named.qualifiedName(), span);
      }
      if (containsPointer(type)) {
        findings.add(
            new LintFinding(
                "PT002",
                Severity.ERROR,
                symbol(),
                "Zeiger (ptr) außerhalb nativen Codes",
                span.toString(),
                "Stelle mit :manual markieren und in manual/ umsetzen"));
      }
    }

    @Override
    protected void onMember(MemberRef ref, SourceSpan span) {
      if (!exempt() && isReflection(ref.symbolId())) {
        reflection(ref.symbolId(), span);
      }
    }

    private void reflection(String target, SourceSpan span) {
      findings.add(
          new LintFinding(
              "PT001",
              Severity.ERROR,
              symbol(),
              "Reflection ohne @manual: " + target,
              span.toString(),
              "Stelle mit :manual markieren und ohne Reflection in manual/ umsetzen"));
    }

    @Override
    protected void onStmt(Stmt stmt) {
      boolean yield = stmt instanceof YieldReturnStmt || stmt instanceof YieldBreakStmt;
      if (!yield || exempt()) {
        return;
      }
      if (insideTryWithFinally()) {
        findings.add(
            new LintFinding(
                "PT003",
                Severity.ERROR,
                symbol(),
                "yield in try mit finally",
                stmt.span().toString(),
                "Coroutine umbauen (finally außerhalb) oder :manual (ADR-008)"));
      } else if (insideLambda()) {
        findings.add(
            new LintFinding(
                "PT003",
                Severity.ERROR,
                symbol(),
                "yield in einem Lambda (verschachtelte Coroutine)",
                stmt.span().toString(),
                "Coroutine in eine eigene Methode auslagern oder :manual (ADR-008)"));
      }
    }
  }
}
