package io.github.localsoul.port.lint;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.ManualNode;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.RawNode;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.StructDecl;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.refs.Declaration;
import io.github.localsoul.port.ir.refs.Declarations;
import io.github.localsoul.port.ir.refs.NodeWalker;
import io.github.localsoul.port.ir.refs.SymbolSource;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Was alle Regeln über das Projekt wissen müssen: deklarierte Symbole mit Ort und Hash, eigene
 * Typen und Structs, Feldtypen und Handarbeitsstellen ({@code :manual} oder {@code raw}/{@code
 * manual} im Rumpf).
 */
final class ProjectModel {

  /** Deklarierte Symbole → Ort. */
  final Map<String, String> locations = new TreeMap<>();

  /** Deklarierte Symbole → Hash der S-Expression (ADR-027). */
  final Map<String, String> hashes = new TreeMap<>();

  /** Eigene Typen. */
  final Set<String> types = new TreeSet<>();

  /** Eigene {@code struct}-Typen. */
  final Set<String> structs = new TreeSet<>();

  /** Eigene Felder → Typ. */
  final Map<String, TypeRef> fieldTypes = new TreeMap<>();

  /** Handarbeitsstellen: Symbol → Ort der ersten Stelle. */
  final Map<String, String> manualSpots = new TreeMap<>();

  private ProjectModel() {}

  static ProjectModel of(LintInput input) {
    ProjectModel model = new ProjectModel();
    for (IrSource source : input.sources()) {
      if (source.file() == null) {
        continue;
      }
      for (Declaration declaration : Declarations.collect(source.file())) {
        SourceSpan span = declaration.span();
        model.locations.putIfAbsent(declaration.symbol(), span.toString());
        model.hashes.putIfAbsent(
            declaration.symbol(), SymbolSource.hash(source.text(), span.line(), span.column()));
      }
      new Builder(model).walk(source.file());
    }
    return model;
  }

  /** {@code true}, wenn das Symbol eine Handarbeitsstelle ist. */
  boolean isManual(String symbol) {
    return manualSpots.containsKey(symbol);
  }

  /** {@code true}, wenn das Symbol oder sein Typ im Projekt deklariert ist. */
  boolean isOwn(String typeName) {
    return types.contains(typeName);
  }

  private static final class Builder extends NodeWalker {

    private final ProjectModel model;

    Builder(ProjectModel model) {
      this.model = model;
    }

    @Override
    protected void onTypeDecl(TypeDecl type) {
      model.types.add(type.name());
      if (type instanceof StructDecl) {
        model.structs.add(type.name());
      }
      if (type.attrs().manual() != null) {
        model.manualSpots.putIfAbsent(symbol(), type.span().toString());
      }
    }

    @Override
    protected void onMemberDecl(MemberDecl decl) {
      if (decl instanceof FieldDecl field) {
        model.fieldTypes.put(symbol(), field.type());
      }
      if (decl.attrs().manual() != null) {
        model.manualSpots.putIfAbsent(symbol(), decl.span().toString());
      }
    }

    @Override
    protected void onStmt(Stmt stmt) {
      handwork(stmt instanceof RawNode || stmt instanceof ManualNode, stmt.span());
    }

    @Override
    protected void onExpr(Expr expr) {
      handwork(expr instanceof RawNode || expr instanceof ManualNode, expr.span());
    }

    private void handwork(boolean found, SourceSpan span) {
      if (found) {
        model.manualSpots.putIfAbsent(symbol(), span.toString());
      }
    }
  }
}
