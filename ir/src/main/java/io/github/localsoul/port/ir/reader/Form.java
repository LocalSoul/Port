package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.NodeAttrs;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SKeyword;
import io.github.localsoul.port.ir.sexpr.SList;
import io.github.localsoul.port.ir.sexpr.SString;
import io.github.localsoul.port.ir.sexpr.SSymbol;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Ein Listenknoten, aufgeteilt in Kopf, Positionsargumente und Keywords.
 *
 * <p>Regeln (Spec Abschnitt 11):
 *
 * <ul>
 *   <li>Bekannte Keywords mit Wert nehmen genau das nächste Element. Fehlt es oder ist es selbst
 *       ein Keyword, ist das ein Fehler. Doppelte bekannte Keywords sind ein Fehler.
 *   <li>Unbekannte Keywords werden ignoriert. Sie nehmen das nächste Element als Wert mit, wenn es
 *       existiert und kein Keyword ist. Künftige Keywords brauchen deshalb immer einen Wert.
 *   <li>{@code :native} leitet den nativen Dialekt ein und wird abgelehnt (bis M10b).
 *   <li>Alle übrigen Elemente sind Positionsargumente, in Eingabereihenfolge.
 * </ul>
 */
final class Form {

  private static final Set<String> COMMON = Set.of("loc", "manual", "note");

  private final SList list;
  private final String head;
  private final List<SExpr> args;
  // Nur für Lookups, nie iteriert: Reihenfolge spielt keine Rolle.
  private final Map<String, SExpr> values;
  private final Set<String> flags;

  private Form(
      SList list, String head, List<SExpr> args, Map<String, SExpr> values, Set<String> flags) {
    this.list = list;
    this.head = head;
    this.args = args;
    this.values = values;
    this.flags = flags;
  }

  /**
   * Zerlegt eine Liste, deren erstes Element ein Symbol ist.
   *
   * @throws IrSyntaxException bei leerer Liste, fehlendem Kopf oder fehlerhaften Keywords
   */
  static Form of(SList list, KeywordSpec spec) throws IrSyntaxException {
    String head = headOf(list);
    List<SExpr> items = list.items();
    List<SExpr> args = new ArrayList<>();
    Map<String, SExpr> values = new HashMap<>();
    Set<String> flags = new HashSet<>();
    int i = 1;
    while (i < items.size()) {
      SExpr item = items.get(i);
      i++;
      if (!(item instanceof SKeyword keyword)) {
        args.add(item);
        continue;
      }
      String name = keyword.name();
      SExpr next = i < items.size() ? items.get(i) : null;
      boolean nextIsValue = next != null && !(next instanceof SKeyword);
      if (name.equals("native")) {
        throw Expect.dialect(keyword.span(), ":native");
      } else if (spec.flagKeywords().contains(name)) {
        checkDuplicate(flags.contains(name), keyword);
        flags.add(name);
      } else if (COMMON.contains(name) || spec.valueKeywords().contains(name)) {
        checkDuplicate(values.containsKey(name), keyword);
        if (!nextIsValue) {
          throw new IrSyntaxException(keyword.span(), "Keyword ':" + name + "' ohne Wert");
        }
        values.put(name, next);
        i++;
      } else if (nextIsValue) {
        i++;
      }
    }
    return new Form(list, head, args, values, flags);
  }

  /** Liefert den Kopf einer Liste, ohne sie zu zerlegen. */
  static String headOf(SList list) throws IrSyntaxException {
    if (list.items().isEmpty()) {
      throw new IrSyntaxException(list.span(), "leere Liste, Knotenname erwartet");
    }
    SExpr first = list.items().get(0);
    if (!(first instanceof SSymbol symbol)) {
      throw new IrSyntaxException(
          first.span(), "Knotenname erwartet, gefunden " + Expect.describe(first));
    }
    return symbol.text();
  }

  private static void checkDuplicate(boolean duplicate, SKeyword keyword) throws IrSyntaxException {
    if (duplicate) {
      throw new IrSyntaxException(keyword.span(), "Keyword ':" + keyword.name() + "' doppelt");
    }
  }

  String head() {
    return head;
  }

  SourceSpan span() {
    return list.span();
  }

  int argCount() {
    return args.size();
  }

  SExpr arg(int index) {
    return args.get(index);
  }

  /** Argumente ab {@code from} bis zum Ende. */
  List<SExpr> argsFrom(int from) {
    return args.subList(from, args.size());
  }

  /** Wert eines bekannten Keywords oder {@code null}. */
  SExpr value(String keyword) {
    return values.get(keyword);
  }

  boolean hasFlag(String keyword) {
    return flags.contains(keyword);
  }

  /**
   * Prüft die Anzahl der Positionsargumente.
   *
   * @param min Minimum
   * @param max Maximum, {@code -1} für unbegrenzt
   * @param usage Kurzform der Syntax für die Meldung, z. B. {@code "(if expr stmt [stmt])"}
   */
  void requireArgs(int min, int max, String usage) throws IrSyntaxException {
    int count = args.size();
    boolean tooFew = count < min;
    boolean tooMany = max >= 0 && count > max;
    if (tooFew || tooMany) {
      throw new IrSyntaxException(
          list.span(),
          "falsche Stelligkeit: '"
              + head
              + "' erwartet "
              + usage
              + ", gefunden "
              + count
              + " Argument(e)");
    }
  }

  /** Liest {@code :loc}, {@code :manual} und {@code :note}. */
  NodeAttrs nodeAttrs() throws IrSyntaxException {
    String loc = optionalString("loc");
    String manual = optionalString("manual");
    String note = optionalString("note");
    if (loc == null && manual == null && note == null) {
      return NodeAttrs.NONE;
    }
    return new NodeAttrs(loc, manual, note);
  }

  /** Wert eines Keywords als String, {@code null}, wenn es fehlt. */
  String optionalString(String keyword) throws IrSyntaxException {
    SExpr value = values.get(keyword);
    if (value == null) {
      return null;
    }
    if (!(value instanceof SString string)) {
      throw new IrSyntaxException(
          value.span(),
          "':" + keyword + "' erwartet einen String, gefunden " + Expect.describe(value));
    }
    return string.value();
  }

  /** Wert eines Pflicht-Keywords. */
  SExpr requiredValue(String keyword) throws IrSyntaxException {
    SExpr value = values.get(keyword);
    if (value == null) {
      throw new IrSyntaxException(list.span(), "'" + head + "' braucht ':" + keyword + "'");
    }
    return value;
  }
}
