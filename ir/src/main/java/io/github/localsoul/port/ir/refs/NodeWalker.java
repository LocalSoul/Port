package io.github.localsoul.port.ir.refs;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.ArrayLitExpr;
import io.github.localsoul.port.ir.ast.AsExpr;
import io.github.localsoul.port.ir.ast.AttributeUse;
import io.github.localsoul.port.ir.ast.BinaryExpr;
import io.github.localsoul.port.ir.ast.BlockStmt;
import io.github.localsoul.port.ir.ast.CallExpr;
import io.github.localsoul.port.ir.ast.CastExpr;
import io.github.localsoul.port.ir.ast.CatchClause;
import io.github.localsoul.port.ir.ast.CheckedExpr;
import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.CoalesceExpr;
import io.github.localsoul.port.ir.ast.CondExpr;
import io.github.localsoul.port.ir.ast.ConvExpr;
import io.github.localsoul.port.ir.ast.CopyExpr;
import io.github.localsoul.port.ir.ast.CtorDecl;
import io.github.localsoul.port.ir.ast.DefaultExpr;
import io.github.localsoul.port.ir.ast.DelegateExpr;
import io.github.localsoul.port.ir.ast.DoWhileStmt;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.EventAddExpr;
import io.github.localsoul.port.ir.ast.EventDecl;
import io.github.localsoul.port.ir.ast.EventRemoveExpr;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.ExprStmt;
import io.github.localsoul.port.ir.ast.FieldDecl;
import io.github.localsoul.port.ir.ast.FieldExpr;
import io.github.localsoul.port.ir.ast.ForStmt;
import io.github.localsoul.port.ir.ast.ForeachStmt;
import io.github.localsoul.port.ir.ast.IfStmt;
import io.github.localsoul.port.ir.ast.IncDecExpr;
import io.github.localsoul.port.ir.ast.IndexExpr;
import io.github.localsoul.port.ir.ast.InterfaceDecl;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.IsExpr;
import io.github.localsoul.port.ir.ast.LambdaExpr;
import io.github.localsoul.port.ir.ast.LocalStmt;
import io.github.localsoul.port.ir.ast.LockStmt;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.MethodDecl;
import io.github.localsoul.port.ir.ast.NewArrayExpr;
import io.github.localsoul.port.ir.ast.NewExpr;
import io.github.localsoul.port.ir.ast.Param;
import io.github.localsoul.port.ir.ast.PropDecl;
import io.github.localsoul.port.ir.ast.PropExpr;
import io.github.localsoul.port.ir.ast.ReturnStmt;
import io.github.localsoul.port.ir.ast.SetExpr;
import io.github.localsoul.port.ir.ast.SetOpExpr;
import io.github.localsoul.port.ir.ast.StaticCallExpr;
import io.github.localsoul.port.ir.ast.StaticFieldExpr;
import io.github.localsoul.port.ir.ast.StaticPropExpr;
import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.StructDecl;
import io.github.localsoul.port.ir.ast.SwitchCase;
import io.github.localsoul.port.ir.ast.SwitchStmt;
import io.github.localsoul.port.ir.ast.SymbolIds;
import io.github.localsoul.port.ir.ast.ThrowStmt;
import io.github.localsoul.port.ir.ast.TryStmt;
import io.github.localsoul.port.ir.ast.TypeAttrs;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.ast.TypeofExpr;
import io.github.localsoul.port.ir.ast.UnaryExpr;
import io.github.localsoul.port.ir.ast.UncheckedExpr;
import io.github.localsoul.port.ir.ast.UsingStmt;
import io.github.localsoul.port.ir.ast.WhileStmt;
import io.github.localsoul.port.ir.ast.YieldReturnStmt;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Durchläuft alle Knoten einer IR-Datei in Eingabereihenfolge und ruft Haken für Typen, Member-
 * Referenzen, Statements und Ausdrücke auf. Unterklassen (z. B. {@link References}, Lint-Regeln)
 * überschreiben nur die Haken, die sie brauchen. Während des Durchlaufs liefern {@link #symbol()},
 * {@link #member()}, {@link #genericScope()}, {@link #insideTryWithFinally()} und {@link
 * #insideLambda()} den Kontext.
 */
public abstract class NodeWalker {

  private String symbol = "";
  private MemberDecl member;
  private Set<String> genericScope = Set.of();
  private int tryWithFinally;
  private int lambdas;

  /** Für Unterklassen. */
  protected NodeWalker() {}

  /**
   * Durchläuft eine Datei.
   *
   * @param file gelesene IR-Datei
   */
  public final void walk(IrFile file) {
    for (TypeDecl type : file.types()) {
      walkType(type);
    }
  }

  /**
   * Symbol-ID des umgebenden Typs oder Mitglieds.
   *
   * @return Symbol-ID
   */
  protected final String symbol() {
    return symbol;
  }

  /**
   * Umgebendes Mitglied.
   *
   * @return Mitglied oder {@code null} auf Typebene
   */
  protected final MemberDecl member() {
    return member;
  }

  /**
   * Sichtbare generische Typparameter (Typ und Methode).
   *
   * @return Namen
   */
  protected final Set<String> genericScope() {
    return genericScope;
  }

  /**
   * {@code true} im geschützten Block oder in einem {@code catch} eines {@code try} mit {@code
   * finally}.
   *
   * @return Kontext
   */
  protected final boolean insideTryWithFinally() {
    return tryWithFinally > 0;
  }

  /**
   * {@code true} im Rumpf eines Lambdas.
   *
   * @return Kontext
   */
  protected final boolean insideLambda() {
    return lambdas > 0;
  }

  /**
   * Haken: eine Typreferenz.
   *
   * @param type Typ
   * @param span Position des Knotens mit der Referenz
   */
  protected void onType(TypeRef type, SourceSpan span) {}

  /**
   * Haken: eine Member-Referenz.
   *
   * @param ref Member
   * @param span Position des Knotens mit der Referenz
   */
  protected void onMember(MemberRef ref, SourceSpan span) {}

  /**
   * Haken: eine Typdeklaration, bevor ihre Mitglieder durchlaufen werden.
   *
   * @param type Typ
   */
  protected void onTypeDecl(TypeDecl type) {}

  /**
   * Haken: eine Mitgliedsdeklaration, bevor sie durchlaufen wird ({@link #member()} ist gesetzt).
   *
   * @param decl Mitglied
   */
  protected void onMemberDecl(MemberDecl decl) {}

  /**
   * Haken: ein Statement, bevor seine Kinder durchlaufen werden.
   *
   * @param stmt Statement
   */
  protected void onStmt(Stmt stmt) {}

  /**
   * Haken: ein Ausdruck, bevor seine Kinder durchlaufen werden.
   *
   * @param expr Ausdruck
   */
  protected void onExpr(Expr expr) {}

  private void walkType(TypeDecl type) {
    symbol = SymbolIds.of(type);
    member = null;
    onTypeDecl(type);
    switch (type) {
      case ClassDecl c -> walkClassLike(c.typeAttrs(), c.members(), c.span());
      case StructDecl s -> walkClassLike(s.typeAttrs(), s.members(), s.span());
      case InterfaceDecl i -> walkClassLike(i.typeAttrs(), i.members(), i.span());
      case EnumDecl e -> {
        genericScope = Set.of();
        type(e.underlyingType(), e.span());
      }
    }
  }

  private void walkClassLike(TypeAttrs attrs, List<MemberDecl> members, SourceSpan span) {
    String typeName = symbol;
    Set<String> typeGenerics = Set.copyOf(attrs.generics());
    genericScope = typeGenerics;
    if (attrs.baseType() != null) {
      type(attrs.baseType(), span);
    }
    for (TypeRef implemented : attrs.implementedTypes()) {
      type(implemented, span);
    }
    attributes(attrs.attributes());
    for (MemberDecl decl : members) {
      symbol = SymbolIds.of(typeName, decl);
      member = decl;
      genericScope = typeGenerics;
      onMemberDecl(decl);
      walkMember(decl);
    }
    symbol = typeName;
    member = null;
  }

  private void walkMember(MemberDecl decl) {
    switch (decl) {
      case FieldDecl f -> {
        type(f.type(), f.span());
        attributes(f.attributes());
        exprIfPresent(f.initializer());
      }
      case PropDecl p -> {
        type(p.type(), p.span());
        stmtIfPresent(p.getter());
        stmtIfPresent(p.setter());
      }
      case EventDecl e -> type(e.type(), e.span());
      case CtorDecl c -> {
        params(c.parameters());
        if (c.baseCallArguments() != null) {
          exprs(c.baseCallArguments());
        }
        stmt(c.body());
      }
      case MethodDecl m -> walkMethod(m);
    }
  }

  private void walkMethod(MethodDecl method) {
    if (!method.generics().isEmpty()) {
      Set<String> scope = new LinkedHashSet<>(genericScope);
      scope.addAll(method.generics());
      genericScope = Set.copyOf(scope);
    }
    type(method.returnType(), method.span());
    params(method.parameters());
    stmtIfPresent(method.body());
  }

  private void params(List<Param> params) {
    for (Param param : params) {
      type(param.type(), param.span());
      exprIfPresent(param.defaultValue());
    }
  }

  private void attributes(List<AttributeUse> attributes) {
    for (AttributeUse attribute : attributes) {
      type(new NamedType(attribute.type(), List.of()), attribute.span());
      exprs(attribute.arguments());
    }
  }

  /** Reine Verteilertabelle über alle Statements. */
  private void stmt(Stmt stmt) {
    onStmt(stmt);
    switch (stmt) {
      case BlockStmt b -> stmts(b.statements());
      case LocalStmt l -> {
        type(l.type(), l.span());
        exprIfPresent(l.initializer());
      }
      case ExprStmt e -> expr(e.expression());
      case IfStmt i -> {
        expr(i.condition());
        stmt(i.thenBranch());
        stmtIfPresent(i.elseBranch());
      }
      case WhileStmt w -> {
        expr(w.condition());
        stmt(w.body());
      }
      case DoWhileStmt d -> {
        stmt(d.body());
        expr(d.condition());
      }
      case ForStmt f -> {
        stmts(f.initializers());
        expr(f.condition());
        exprs(f.updates());
        stmt(f.body());
      }
      case ForeachStmt f -> {
        type(f.type(), f.span());
        expr(f.collection());
        stmt(f.body());
      }
      case SwitchStmt s -> walkSwitch(s);
      case ReturnStmt r -> exprIfPresent(r.value());
      case ThrowStmt t -> expr(t.exception());
      case TryStmt t -> walkTry(t);
      case YieldReturnStmt y -> expr(y.value());
      case LockStmt l -> {
        expr(l.lockObject());
        stmt(l.body());
      }
      case UsingStmt u -> {
        type(u.type(), u.span());
        expr(u.resource());
        stmt(u.body());
      }
      default -> {
        // break, continue, yield-break, label, goto, raw, manual: keine Referenzen
      }
    }
  }

  private void walkSwitch(SwitchStmt stmt) {
    expr(stmt.value());
    for (SwitchCase branch : stmt.cases()) {
      exprs(branch.labels());
      stmts(branch.body());
    }
    if (stmt.defaultCase() != null) {
      stmts(stmt.defaultCase().body());
    }
  }

  private void walkTry(TryStmt stmt) {
    boolean guarded = stmt.finallyClause() != null;
    if (guarded) {
      tryWithFinally++;
    }
    stmt(stmt.body());
    for (CatchClause clause : stmt.catches()) {
      type(clause.type(), clause.span());
      stmt(clause.body());
    }
    if (guarded) {
      tryWithFinally--;
    }
    if (stmt.finallyClause() != null) {
      stmt(stmt.finallyClause().body());
    }
  }

  /** Reine Verteilertabelle über alle Ausdrücke mit Kindern oder Referenzen. */
  private void expr(Expr expr) {
    onExpr(expr);
    switch (expr) {
      case FieldExpr f -> {
        expr(f.target());
        member(f.field(), f.span());
      }
      case StaticFieldExpr f -> member(f.field(), f.span());
      case PropExpr p -> {
        expr(p.target());
        member(p.property(), p.span());
      }
      case StaticPropExpr p -> member(p.property(), p.span());
      case CallExpr c -> {
        expr(c.target());
        member(c.method(), c.span());
        exprs(c.arguments());
      }
      case StaticCallExpr c -> {
        member(c.method(), c.span());
        exprs(c.arguments());
      }
      case NewExpr n -> {
        member(n.constructor(), n.span());
        exprs(n.arguments());
      }
      case NewArrayExpr n -> {
        type(n.elementType(), n.span());
        exprs(n.lengths());
      }
      case ArrayLitExpr a -> {
        type(a.elementType(), a.span());
        exprs(a.elements());
      }
      case IndexExpr i -> {
        expr(i.target());
        exprs(i.indices());
      }
      case BinaryExpr b -> {
        expr(b.left());
        expr(b.right());
      }
      case UnaryExpr u -> expr(u.operand());
      case CheckedExpr c -> expr(c.operand());
      case UncheckedExpr u -> expr(u.operand());
      case CopyExpr c -> expr(c.operand());
      case SetExpr s -> {
        expr(s.target());
        expr(s.value());
      }
      case SetOpExpr s -> {
        expr(s.target());
        expr(s.value());
      }
      case IncDecExpr i -> expr(i.target());
      case ConvExpr c -> typedOperand(c.type(), c.operand(), c.span());
      case CastExpr c -> typedOperand(c.type(), c.operand(), c.span());
      case AsExpr a -> typedOperand(a.type(), a.operand(), a.span());
      case IsExpr i -> typedOperand(i.type(), i.operand(), i.span());
      case CondExpr c -> {
        expr(c.condition());
        expr(c.whenTrue());
        expr(c.whenFalse());
      }
      case CoalesceExpr c -> {
        expr(c.left());
        expr(c.right());
      }
      case TypeofExpr t -> type(t.type(), t.span());
      case DefaultExpr d -> type(d.type(), d.span());
      case LambdaExpr l -> {
        params(l.parameters());
        lambdas++;
        stmt(l.body());
        lambdas--;
      }
      case DelegateExpr d -> {
        expr(d.target());
        member(d.method(), d.span());
      }
      case EventAddExpr e -> eventAccess(e.target(), e.event(), e.handler(), e.span());
      case EventRemoveExpr e -> eventAccess(e.target(), e.event(), e.handler(), e.span());
      default -> {
        // Literale, Bezeichner, this, base, raw, manual: keine Referenzen
      }
    }
  }

  private void typedOperand(TypeRef type, Expr operand, SourceSpan span) {
    type(type, span);
    expr(operand);
  }

  private void eventAccess(Expr target, MemberRef event, Expr handler, SourceSpan span) {
    expr(target);
    member(event, span);
    expr(handler);
  }

  private void stmts(List<Stmt> stmts) {
    for (Stmt stmt : stmts) {
      stmt(stmt);
    }
  }

  private void exprs(List<Expr> exprs) {
    for (Expr expr : exprs) {
      expr(expr);
    }
  }

  private void stmtIfPresent(Stmt stmt) {
    if (stmt != null) {
      stmt(stmt);
    }
  }

  private void exprIfPresent(Expr expr) {
    if (expr != null) {
      expr(expr);
    }
  }

  private void type(TypeRef type, SourceSpan span) {
    onType(type, span);
  }

  private void member(MemberRef ref, SourceSpan span) {
    onMember(ref, span);
  }
}
