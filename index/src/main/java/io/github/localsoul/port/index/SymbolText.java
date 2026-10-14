package io.github.localsoul.port.index;

/**
 * Schneidet den Quelltext einer S-Expression aus einer IR-Datei: ab Zeile und Spalte der öffnenden
 * Klammer bis zur passenden schließenden, Klammern in Strings und Kommentaren ({@code ;}) zählen
 * nicht.
 */
final class SymbolText {

  private SymbolText() {}

  /**
   * Liefert den Ausschnitt.
   *
   * @param text Inhalt der Datei
   * @param line Zeile ab 1
   * @param column Spalte ab 1
   * @return Ausschnitt; bei ungültiger Position oder ohne Klammer der Rest der Zeile
   */
  static String slice(String text, int line, int column) {
    int start = offset(text, line, column);
    if (start < 0 || start >= text.length()) {
      return "";
    }
    if (text.charAt(start) != '(') {
      int end = text.indexOf('\n', start);
      return text.substring(start, end < 0 ? text.length() : end);
    }
    int depth = 0;
    int i = start;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '"') {
        i = skipString(text, i);
        continue;
      }
      if (c == ';') {
        int end = text.indexOf('\n', i);
        i = end < 0 ? text.length() : end;
        continue;
      }
      if (c == '(') {
        depth++;
      } else if (c == ')') {
        depth--;
        if (depth == 0) {
          return text.substring(start, i + 1);
        }
      }
      i++;
    }
    return text.substring(start);
  }

  private static int skipString(String text, int quote) {
    int i = quote + 1;
    while (i < text.length()) {
      char c = text.charAt(i);
      if (c == '\\') {
        i += 2;
        continue;
      }
      if (c == '"') {
        return i + 1;
      }
      i++;
    }
    return i;
  }

  private static int offset(String text, int line, int column) {
    int current = 1;
    int offset = 0;
    while (current < line) {
      int newline = text.indexOf('\n', offset);
      if (newline < 0) {
        return -1;
      }
      offset = newline + 1;
      current++;
    }
    return offset + column - 1;
  }
}
