package io.github.localsoul.port.generator.java;

/** Schreibt Zeilen mit Einrückung (2 Leerzeichen je Ebene, google-java-format). */
final class JavaWriter {

  private final StringBuilder out = new StringBuilder();
  private int level;

  /** Eine Zeile auf aktueller Ebene; Zeilenumbrüche im Text werden mit eingerückt. */
  void line(String text) {
    for (String part : text.split("\n", -1)) {
      if (part.isEmpty()) {
        out.append('\n');
      } else {
        out.append("  ".repeat(level)).append(part).append('\n');
      }
    }
  }

  void blank() {
    out.append('\n');
  }

  /** {@code header {} und eine Ebene tiefer. */
  void open(String header) {
    line(header + " {");
    level++;
  }

  /** Schließt mit {@code suffix} (z. B. {@code "}"} oder {@code "} else {"}). */
  void close(String suffix) {
    level--;
    line(suffix);
  }

  /** Eine Ebene tiefer, ohne Zeile. */
  void push() {
    level++;
  }

  /** Eine Ebene höher, ohne Zeile. */
  void pop() {
    level--;
  }

  int level() {
    return level;
  }

  /** Einrückung in Spalten der aktuellen Ebene. */
  int indent() {
    return level * 2;
  }

  /**
   * Block {@code header { inner }}; ist {@code inner} leer, schreibt google-java-format {@code
   * header {}} in einer Zeile.
   */
  void block(String header, JavaWriter inner) {
    if (inner.out.isEmpty()) {
      line(header + " {}");
      return;
    }
    line(header + " {");
    append(inner);
    line("}");
  }

  /** Neuer Schreiber eine Ebene tiefer als dieser, für Rümpfe. */
  JavaWriter nested() {
    JavaWriter writer = new JavaWriter();
    writer.level = level + 1;
    return writer;
  }

  /** Hängt fertig eingerückten Text eines anderen Schreibers an. */
  void append(JavaWriter other) {
    out.append(other.out);
  }

  /** Erzeugt einen Schreiber auf derselben Ebene. */
  JavaWriter sibling() {
    JavaWriter writer = new JavaWriter();
    writer.level = level;
    return writer;
  }

  String text() {
    return out.toString();
  }
}
