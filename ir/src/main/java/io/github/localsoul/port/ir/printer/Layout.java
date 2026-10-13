package io.github.localsoul.port.ir.printer;

import java.util.List;

/**
 * Setzt ein {@link Doc} in Text.
 *
 * <p>Zeilenregel (auch in {@code docs/modules/ir.md}):
 *
 * <ol>
 *   <li>Eine Liste steht in einer Zeile, wenn sie nicht zum Umbruch gezwungen ist (kein Element
 *       erzwingt einen Umbruch) und ihre einzeilige Form ab ihrer Startspalte höchstens {@value
 *       #WIDTH} Zeichen erreicht. Schließende Klammern umgebender Listen zählen nicht mit.
 *   <li>Sonst beginnt die erste Zeile mit {@code (kopf}. Danach folgen der Reihe nach Elemente in
 *       derselben Zeile, solange sie einzeilig passen, nicht zum Umbruch gezwungen sind und
 *       höchstens {@code headCount} Elemente erreicht sind.
 *   <li>Jedes weitere Element steht auf einer eigenen Zeile, um 2 Leerzeichen gegenüber der Zeile
 *       der öffnenden Klammer eingerückt. Die schließende Klammer folgt direkt dem letzten Element.
 * </ol>
 */
final class Layout {

  /** Maximale Zeilenbreite für einzeilige Listen. */
  static final int WIDTH = 100;

  private static final int INDENT = 2;

  private Layout() {}

  /**
   * Setzt ein Dokument.
   *
   * @param doc Dokument
   * @param column Spalte (ab 0), an der das Dokument beginnt
   * @param indent Einrückung der Zeile, in der das Dokument beginnt
   * @return Text ohne abschließenden Zeilenumbruch
   */
  static String render(Doc doc, int column, int indent) {
    if (doc instanceof Doc.Atom atom) {
      return atom.text();
    }
    Doc.Group group = (Doc.Group) doc;
    String flat = flat(group);
    if (!mustBreak(group) && column + flat.length() <= WIDTH) {
      return flat;
    }
    return renderBroken(group, column, indent);
  }

  private static String renderBroken(Doc.Group group, int column, int indent) {
    StringBuilder out = new StringBuilder();
    out.append('(').append(group.head());
    int currentColumn = column + 1 + group.head().length();
    boolean lineEmpty = group.head().isEmpty();
    List<Doc.Item> items = group.items();
    int next = 0;
    while (next < items.size() && next < group.headCount()) {
      Doc.Item item = items.get(next);
      String itemFlat = flatItem(item);
      int separator = lineEmpty ? 0 : 1;
      boolean fits = currentColumn + separator + itemFlat.length() <= WIDTH;
      if (mustBreak(item.value()) || !fits) {
        break;
      }
      if (!lineEmpty) {
        out.append(' ');
      }
      out.append(itemFlat);
      currentColumn += separator + itemFlat.length();
      lineEmpty = false;
      next++;
    }
    int childIndent = indent + INDENT;
    for (int i = next; i < items.size(); i++) {
      out.append('\n').append(" ".repeat(childIndent));
      out.append(renderItem(items.get(i), childIndent));
    }
    out.append(')');
    return out.toString();
  }

  private static String renderItem(Doc.Item item, int indent) {
    if (item.keyword() == null) {
      return render(item.value(), indent, indent);
    }
    String prefix = ":" + item.keyword() + " ";
    return prefix + render(item.value(), indent + prefix.length(), indent);
  }

  /** Einzeilige Form. */
  static String flat(Doc doc) {
    if (doc instanceof Doc.Atom atom) {
      return atom.text();
    }
    Doc.Group group = (Doc.Group) doc;
    StringBuilder out = new StringBuilder();
    out.append('(').append(group.head());
    boolean first = group.head().isEmpty();
    for (Doc.Item item : group.items()) {
      if (!first) {
        out.append(' ');
      }
      out.append(flatItem(item));
      first = false;
    }
    out.append(')');
    return out.toString();
  }

  private static String flatItem(Doc.Item item) {
    if (item.keyword() == null) {
      return flat(item.value());
    }
    return ":" + item.keyword() + " " + flat(item.value());
  }

  /** {@code true}, wenn die Liste oder ein enthaltenes Element einen Umbruch erzwingt. */
  private static boolean mustBreak(Doc doc) {
    if (!(doc instanceof Doc.Group group)) {
      return false;
    }
    if (group.forceBreak()) {
      return true;
    }
    for (Doc.Item item : group.items()) {
      if (mustBreak(item.value())) {
        return true;
      }
    }
    return false;
  }
}
