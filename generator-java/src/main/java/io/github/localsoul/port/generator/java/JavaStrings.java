package io.github.localsoul.port.generator.java;

/** Java-String-Literale. */
final class JavaStrings {

  private JavaStrings() {}

  /**
   * Schreibt {@code "..."}. Steuerzeichen als Oktal-Escape (ein {@code \}{@code u000A} würde javac
   * schon vor dem Lexen in einen Zeilenumbruch verwandeln), einzelne Surrogate als Unicode-Escape,
   * alles andere unverändert (Quelltext ist UTF-8).
   */
  static String quote(String value) {
    StringBuilder out = new StringBuilder("\"");
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
    return out.append('"').toString();
  }

  private static void appendOther(StringBuilder out, String value, int index, char c) {
    if (c < 0x20 || c == 0x7F) {
      out.append('\\').append(Integer.toOctalString(c | 0x200).substring(1));
    } else if (Character.isSurrogate(c) && !isPaired(value, index, c)) {
      out.append(String.format(java.util.Locale.ROOT, "\\u%04X", (int) c));
    } else {
      out.append(c);
    }
  }

  private static boolean isPaired(String value, int index, char c) {
    if (Character.isHighSurrogate(c)) {
      return index + 1 < value.length() && Character.isLowSurrogate(value.charAt(index + 1));
    }
    return index > 0 && Character.isHighSurrogate(value.charAt(index - 1));
  }
}
