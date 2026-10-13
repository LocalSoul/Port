package io.github.localsoul.port.ir.printer;

import java.util.Locale;

/** Schreibt String-Literale mit den Escapes aus Spec Abschnitt 2. */
final class StringEscapes {

  private StringEscapes() {}

  /**
   * Liefert {@code "..."} mit Escapes. {@code \\ \" \n \r \t} haben Kurzformen. Andere
   * Steuerzeichen (unter U+0020 und U+007F) und einzelne Surrogate werden als {@code \}{@code
   * uXXXX} geschrieben, damit die Ausgabe gültiges UTF-8 bleibt. Alles andere bleibt unverändert.
   */
  static String quote(String value) {
    StringBuilder out = new StringBuilder(value.length() + 2);
    out.append('"');
    for (int i = 0; i < value.length(); i++) {
      char c = value.charAt(i);
      switch (c) {
        case '\\' -> out.append("\\\\");
        case '"' -> out.append("\\\"");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> appendOther(out, value, i, c);
      }
    }
    out.append('"');
    return out.toString();
  }

  private static void appendOther(StringBuilder out, String value, int index, char c) {
    boolean control = c < 0x20 || c == 0x7F;
    boolean loneSurrogate = isLoneSurrogate(value, index, c);
    if (control || loneSurrogate) {
      out.append(String.format(Locale.ROOT, "\\u%04X", (int) c));
    } else {
      out.append(c);
    }
  }

  private static boolean isLoneSurrogate(String value, int index, char c) {
    if (Character.isHighSurrogate(c)) {
      boolean hasLow =
          index + 1 < value.length() && Character.isLowSurrogate(value.charAt(index + 1));
      return !hasLow;
    }
    if (Character.isLowSurrogate(c)) {
      boolean hasHigh = index > 0 && Character.isHighSurrogate(value.charAt(index - 1));
      return !hasHigh;
    }
    return false;
  }
}
