package io.github.localsoul.port.ir.printer;

import io.github.localsoul.port.ir.ast.ArrayLitExpr;
import io.github.localsoul.port.ir.ast.AsExpr;
import io.github.localsoul.port.ir.ast.BaseExpr;
import io.github.localsoul.port.ir.ast.BinaryExpr;
import io.github.localsoul.port.ir.ast.BoolLit;
import io.github.localsoul.port.ir.ast.CallExpr;
import io.github.localsoul.port.ir.ast.CastExpr;
import io.github.localsoul.port.ir.ast.CharLit;
import io.github.localsoul.port.ir.ast.CheckedExpr;
import io.github.localsoul.port.ir.ast.CoalesceExpr;
import io.github.localsoul.port.ir.ast.CondExpr;
import io.github.localsoul.port.ir.ast.ConvExpr;
import io.github.localsoul.port.ir.ast.CopyExpr;
import io.github.localsoul.port.ir.ast.DefaultExpr;
import io.github.localsoul.port.ir.ast.DelegateExpr;
import io.github.localsoul.port.ir.ast.EventAddExpr;
import io.github.localsoul.port.ir.ast.EventRemoveExpr;
import io.github.localsoul.port.ir.ast.Expr;
import io.github.localsoul.port.ir.ast.FieldExpr;
import io.github.localsoul.port.ir.ast.IdentExpr;
import io.github.localsoul.port.ir.ast.IncDecExpr;
import io.github.localsoul.port.ir.ast.IndexExpr;
import io.github.localsoul.port.ir.ast.IsExpr;
import io.github.localsoul.port.ir.ast.LambdaExpr;
import io.github.localsoul.port.ir.ast.ManualNode;
import io.github.localsoul.port.ir.ast.NewArrayExpr;
import io.github.localsoul.port.ir.ast.NewExpr;
import io.github.localsoul.port.ir.ast.NodeAttrs;
import io.github.localsoul.port.ir.ast.NullLit;
import io.github.localsoul.port.ir.ast.NumberLit;
import io.github.localsoul.port.ir.ast.PropExpr;
import io.github.localsoul.port.ir.ast.RawNode;
import io.github.localsoul.port.ir.ast.SetExpr;
import io.github.localsoul.port.ir.ast.SetOpExpr;
import io.github.localsoul.port.ir.ast.StaticCallExpr;
import io.github.localsoul.port.ir.ast.StaticFieldExpr;
import io.github.localsoul.port.ir.ast.StaticPropExpr;
import io.github.localsoul.port.ir.ast.StringLit;
import io.github.localsoul.port.ir.ast.ThisExpr;
import io.github.localsoul.port.ir.ast.TypeofExpr;
import io.github.localsoul.port.ir.ast.UnaryExpr;
import io.github.localsoul.port.ir.ast.UncheckedExpr;
import java.util.ArrayList;
import java.util.List;

/** Übersetzt Ausdrücke in {@link Doc}. */
final class ExprDocs {

  private ExprDocs() {}

  static List<Doc> of(List<Expr> exprs) {
    List<Doc> result = new ArrayList<>();
    for (Expr expr : exprs) {
      result.add(of(expr));
    }
    return result;
  }

  /** Reine Verteilertabelle über alle Ausdrucksknoten. */
  static Doc of(Expr expr) {
    return switch (expr) {
      case NumberLit n -> literal(n.text(), n.attrs());
      case StringLit s -> literal(StringEscapes.quote(s.value()), s.attrs());
      case BoolLit b -> literal(b.value() ? "true" : "false", b.attrs());
      case NullLit n -> literal("null", n.attrs());
      case IdentExpr i -> literal(i.name(), i.attrs());
      case ThisExpr t -> literal("this", t.attrs());
      case BaseExpr b -> literal("base", b.attrs());
      case CharLit c ->
          GroupBuilder.named("char").atom(Integer.toString(c.value())).attrs(c.attrs()).build();
      case FieldExpr f ->
          GroupBuilder.named("field")
              .doc(of(f.target()))
              .atom(f.field().toString())
              .attrs(f.attrs())
              .build();
      case StaticFieldExpr f ->
          GroupBuilder.named("sfield").atom(f.field().toString()).attrs(f.attrs()).build();
      case PropExpr p ->
          GroupBuilder.named("prop")
              .doc(of(p.target()))
              .atom(p.property().toString())
              .attrs(p.attrs())
              .build();
      case StaticPropExpr p ->
          GroupBuilder.named("sprop").atom(p.property().toString()).attrs(p.attrs()).build();
      case CallExpr c -> call(c);
      case StaticCallExpr c ->
          GroupBuilder.named("scall")
              .atom(c.method().toString())
              .docs(of(c.arguments()))
              .attrs(c.attrs())
              .build();
      case NewExpr n ->
          GroupBuilder.named("new")
              .atom(n.constructor().toString())
              .docs(of(n.arguments()))
              .attrs(n.attrs())
              .build();
      case NewArrayExpr n ->
          GroupBuilder.named("new-array")
              .atom(n.elementType().toString())
              .docs(of(n.lengths()))
              .attrs(n.attrs())
              .build();
      case ArrayLitExpr a ->
          GroupBuilder.named("array-lit")
              .atom(a.elementType().toString())
              .docs(of(a.elements()))
              .attrs(a.attrs())
              .build();
      case IndexExpr i ->
          GroupBuilder.named("index")
              .doc(of(i.target()))
              .docs(of(i.indices()))
              .attrs(i.attrs())
              .build();
      case BinaryExpr b ->
          GroupBuilder.named(b.op().spelling())
              .doc(of(b.left()))
              .doc(of(b.right()))
              .attrs(b.attrs())
              .build();
      case UnaryExpr u -> unary(u.op().spelling(), u.operand(), u.attrs());
      case CheckedExpr c -> unary("checked", c.operand(), c.attrs());
      case UncheckedExpr u -> unary("unchecked", u.operand(), u.attrs());
      case CopyExpr c -> unary("copy", c.operand(), c.attrs());
      case SetExpr s ->
          GroupBuilder.named("set").doc(of(s.target())).doc(of(s.value())).attrs(s.attrs()).build();
      case SetOpExpr s ->
          GroupBuilder.named("set-op")
              .atom(s.op().spelling())
              .doc(of(s.target()))
              .doc(of(s.value()))
              .attrs(s.attrs())
              .build();
      case IncDecExpr i -> unary(i.op().spelling(), i.target(), i.attrs());
      case ConvExpr c -> typed("conv", c.type().toString(), c.operand(), c.attrs());
      case CastExpr c -> typed("cast", c.type().toString(), c.operand(), c.attrs());
      case AsExpr a -> typed("as", a.type().toString(), a.operand(), a.attrs());
      case IsExpr i -> isExpr(i);
      case CondExpr c ->
          GroupBuilder.named("cond")
              .doc(of(c.condition()))
              .doc(of(c.whenTrue()))
              .doc(of(c.whenFalse()))
              .attrs(c.attrs())
              .build();
      case CoalesceExpr c ->
          GroupBuilder.named("coalesce")
              .doc(of(c.left()))
              .doc(of(c.right()))
              .attrs(c.attrs())
              .build();
      case TypeofExpr t ->
          GroupBuilder.named("typeof").atom(t.type().toString()).attrs(t.attrs()).build();
      case DefaultExpr d ->
          GroupBuilder.named("default").atom(d.type().toString()).attrs(d.attrs()).build();
      case LambdaExpr l ->
          GroupBuilder.named("lambda")
              .doc(DeclDocs.params(l.parameters()))
              .doc(StmtDocs.of(l.body()))
              .attrs(l.attrs())
              .build();
      case DelegateExpr d ->
          GroupBuilder.named("delegate")
              .doc(of(d.target()))
              .atom(d.method().toString())
              .attrs(d.attrs())
              .build();
      case EventAddExpr e ->
          event("event-add", e.target(), e.event().toString(), e.handler(), e.attrs());
      case EventRemoveExpr e ->
          event("event-remove", e.target(), e.event().toString(), e.handler(), e.attrs());
      case RawNode r -> raw(r);
      case ManualNode m -> manual(m);
    };
  }

  static Doc raw(RawNode raw) {
    return GroupBuilder.named("raw")
        .atom(raw.language().spelling())
        .atom(StringEscapes.quote(raw.text()))
        .attrs(raw.attrs())
        .build();
  }

  static Doc manual(ManualNode manual) {
    return GroupBuilder.named("manual")
        .atom(StringEscapes.quote(manual.reason()))
        .attrs(manual.attrs())
        .build();
  }

  /**
   * Atom ohne Attribute. Literale und Bezeichner sind in der Spec Atome und können keine Attribute
   * tragen; der Reader erzeugt sie deshalb immer mit {@link NodeAttrs#NONE}.
   */
  private static Doc literal(String text, NodeAttrs attrs) {
    if (!attrs.isEmpty()) {
      throw new IllegalArgumentException(
          "Atom-Ausdruck mit Attributen kann nicht gedruckt werden: " + text);
    }
    return new Doc.Atom(text);
  }

  private static Doc call(CallExpr call) {
    return GroupBuilder.named("call")
        .doc(of(call.target()))
        .atom(call.method().toString())
        .docs(of(call.arguments()))
        .attrs(call.attrs())
        .build();
  }

  private static Doc unary(String head, Expr operand, NodeAttrs attrs) {
    return GroupBuilder.named(head).doc(of(operand)).attrs(attrs).build();
  }

  private static Doc typed(String head, String type, Expr operand, NodeAttrs attrs) {
    return GroupBuilder.named(head).atom(type).doc(of(operand)).attrs(attrs).build();
  }

  private static Doc isExpr(IsExpr is) {
    GroupBuilder builder =
        GroupBuilder.named("is").atom(is.type().toString()).doc(of(is.operand()));
    if (is.binding() != null) {
      builder.atom(is.binding());
    }
    return builder.attrs(is.attrs()).build();
  }

  private static Doc event(String head, Expr target, String event, Expr handler, NodeAttrs attrs) {
    return GroupBuilder.named(head)
        .doc(of(target))
        .atom(event)
        .doc(of(handler))
        .attrs(attrs)
        .build();
  }
}
