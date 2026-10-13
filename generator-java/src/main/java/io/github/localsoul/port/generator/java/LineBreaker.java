package io.github.localsoul.port.generator.java;

import java.util.List;

/**
 * Zeilenumbruch nach google-java-format für die Formen, die der Generator erzeugt (geprüft gegen
 * google-java-format in {@code JavaGeneratorGoldenTest}):
 *
 * <ol>
 *   <li>Passt die Zeile in {@value #WIDTH} Zeichen, bleibt sie einzeilig.
 *   <li>Kette: Bei einfachem Empfänger ({@code this}, Name) bleibt das erste Glied in der ersten
 *       Zeile; jedes weitere Glied beginnt eine neue Zeile mit {@code +4} Einrückung vor dem {@code
 *       .}.
 *   <li>Aufruf: Umbruch nach {@code (}, Argumente mit {@code +4}; passen sie zusammen in eine
 *       Zeile, stehen sie dort, sonst je Argument eine Zeile. Die schließende Klammer folgt dem
 *       letzten Argument.
 *   <li>Unteilbarer Text (z. B. ein langer String) bleibt auch über 100 Zeichen ganz.
 * </ol>
 *
 * <p>Nicht abgedeckt (bisher nicht erzeugt): Umbrüche an binären Operatoren und in Block-Köpfen.
 */
final class LineBreaker {

  /** Maximale Zeilenlänge von google-java-format. */
  static final int WIDTH = 100;

  private static final int CONTINUATION = 4;

  /**
   * Setzt ein Statement {@code prefix + expr + suffix} ab Einrückung {@code indent}.
   *
   * @return Text; Folgezeilen sind relativ zu {@code indent} eingerückt
   */
  String statement(String prefix, Doc expr, String suffix, int indent) {
    String flat = prefix + Doc.flat(expr) + suffix;
    if (indent + flat.length() <= WIDTH) {
      return flat;
    }
    String rendered =
        prefix + render(expr, indent + prefix.length(), indent, suffix.length()) + suffix;
    return relative(rendered, indent);
  }

  /** Folgezeilen relativ zur Einrückung des Statements; der {@link JavaWriter} rückt selbst ein. */
  private static String relative(String text, int indent) {
    String[] lines = text.split("\n", -1);
    StringBuilder out = new StringBuilder(lines[0]);
    for (int i = 1; i < lines.length; i++) {
      out.append('\n').append(lines[i].substring(indent));
    }
    return out.toString();
  }

  /** Javadoc-Einzeiler oder, wenn zu lang, dreizeilig (Text selbst bleibt ungeteilt). */
  static String javadoc(String text, int indent) {
    String single = "/** " + text + " */";
    if (indent + single.length() <= WIDTH) {
      return single;
    }
    return "/**\n * " + text + "\n */";
  }

  /**
   * Setzt {@code doc} ab Spalte {@code column}; {@code continuation} ist die Einrückung der Zeile,
   * in der {@code doc} beginnt; {@code trailing} Zeichen folgen nach dem Ausdruck.
   */
  private String render(Doc doc, int column, int continuation, int trailing) {
    String flat = Doc.flat(doc);
    if (column + flat.length() + trailing <= WIDTH) {
      return flat;
    }
    return switch (doc) {
      case Doc.Text t -> t.text();
      case Doc.Call c -> call(c.head(), c.args(), continuation, trailing);
      case Doc.Chain c -> chain(c, column, continuation, trailing);
    };
  }

  private String call(String head, List<Doc> args, int continuation, int trailing) {
    if (args.isEmpty()) {
      return head + "()";
    }
    int argIndent = continuation + CONTINUATION;
    String pad = " ".repeat(argIndent);
    String together = Doc.joinFlat(args);
    if (argIndent + together.length() + 1 + trailing <= WIDTH) {
      return head + "(\n" + pad + together + ")";
    }
    StringBuilder out = new StringBuilder(head).append("(\n");
    for (int i = 0; i < args.size(); i++) {
      boolean last = i == args.size() - 1;
      int after = last ? 1 + trailing : 1;
      out.append(pad).append(render(args.get(i), argIndent, argIndent, after));
      out.append(last ? ")" : ",\n");
    }
    return out.toString();
  }

  private String chain(Doc.Chain chain, int column, int continuation, int trailing) {
    List<Doc> segments = chain.segments();
    int segmentIndent = continuation + CONTINUATION;
    String pad = " ".repeat(segmentIndent);
    boolean simpleFirst = chain.first() instanceof Doc.Text;
    StringBuilder out = new StringBuilder(Doc.flat(chain.first()));
    int start = 0;
    if (simpleFirst && !segments.isEmpty()) {
      int lastIndex = segments.size() - 1;
      int firstTrailing = lastIndex == 0 ? trailing : 0;
      int firstColumn = column + out.length();
      out.append(render(segments.get(0), firstColumn, continuation, firstTrailing));
      start = 1;
    }
    for (int i = start; i < segments.size(); i++) {
      boolean last = i == segments.size() - 1;
      out.append('\n').append(pad);
      out.append(render(segments.get(i), segmentIndent, segmentIndent, last ? trailing : 0));
    }
    return out.toString();
  }
}
