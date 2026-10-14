package io.github.localsoul.port.lint;

import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.FieldExpr;
import io.github.localsoul.port.ir.ast.IdentExpr;
import io.github.localsoul.port.ir.ast.LocalStmt;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.ReturnStmt;
import io.github.localsoul.port.ir.ast.SetExpr;
import io.github.localsoul.port.ir.ast.StaticFieldExpr;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.refs.NodeWalker;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * SM001: Ein Wert eines eigenen {@code struct}-Typs wird kopiert ({@code set}, {@code local} mit
 * Initialisierer, {@code return}) – in C# eine Kopie, im Java-Backend eine Referenz (ADR-009 OPEN).
 * Erkannt, wenn der Wert eine Variable, ein Parameter oder ein Feld dieses Typs ist; {@code (copy
 * ...)} und {@code new} sind keine Meldung wert.
 */
final class StructCopyRule implements Rule {

  @Override
  public void check(LintInput input, ProjectModel model, List<LintFinding> findings) {
    if (model.structs.isEmpty()) {
      return;
    }
    for (IrSource source : input.sources()) {
      if (source.file() != null) {
        new Walker(model, findings).walk(source.file());
      }
    }
  }

  private static final class Walker extends NodeWalker {

    private final ProjectModel model;
    private final List<LintFinding> findings;
    // Variablen und Parameter des aktuellen Members → Typ (nur für Nachschlagen).
    private final Map<String, TypeRef> variables = new HashMap<>();

    Walker(ProjectModel model, List<LintFinding> findings) {
      this.model = model;
      this.findings = findings;
    }

    @Override
    protected void onMemberDecl(MemberDecl decl) {
      variables.clear();
      List<Param> params =
          switch (decl) {
            case MethodDecl m -> m.parameters();
            case CtorDecl c -> c.parameters();
            default -> List.of();
          };
      for (Param param : params) {
        variables.put(param.name(), param.type());
      }
    }

    @Override
    protected void onStmt(Stmt stmt) {
      if (stmt instanceof LocalStmt local) {
        variables.put(local.name(), local.type());
        check(local.initializer());
      } else if (stmt instanceof ReturnStmt ret) {
        check(ret.value());
      }
    }

    @Override
    protected void onExpr(Expr expr) {
      if (expr instanceof SetExpr set) {
        check(set.value());
      }
    }

    private void check(Expr value) {
      if (value == null) {
        return;
      }
      String struct = structOf(value);
      if (struct != null) {
        findings.add(
            new LintFinding(
                "SM001",
                Severity.WARN,
                symbol(),
                "struct-Wert (" + struct + ") wird kopiert, Zielklasse ist Referenztyp",
                value.span().toString(),
                "als record oder mit Kopiermethode abbilden, (copy ...) verwenden (ADR-009)"));
      }
    }

    /** Struct-Typ des Werts, wenn er aus einer Variablen oder einem Feld stammt. */
    private String structOf(Expr value) {
      TypeRef type =
          switch (value) {
            case IdentExpr i -> variables.get(i.name());
            case FieldExpr f -> model.fieldTypes.get(f.field().symbolId());
            case StaticFieldExpr f -> model.fieldTypes.get(f.field().symbolId());
            default -> null;
          };
      if (type instanceof NamedType named && model.structs.contains(named.qualifiedName())) {
        return named.qualifiedName();
      }
      return null;
    }
  }
}
