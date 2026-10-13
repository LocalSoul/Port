package io.github.localsoul.port.json;

import java.util.List;

/**
 * Schreibt JSON deterministisch: zwei Leerzeichen Einrückung, {@code ": "} nach Schlüsseln, leere
 * Objekte/Listen als {@code {}}/{@code []}, Zeilenende {@code \n}. Escaping nach RFC 8259 (nur
 * Pflicht-Escapes, Steuerzeichen als {@code \}{@code uXXXX}).
 */
public final class JsonWriter {

  private JsonWriter() {}

  /**
   * Schreibt einen Wert mit abschließendem Zeilenumbruch.
   *
   * @param value Wert
   * @return JSON-Text
   */
  public static String write(JsonValue value) {
    StringBuilder out = new StringBuilder();
    write(value, 0, out);
    return out.append('\n').toString();
  }

  private static void write(JsonValue value, int depth, StringBuilder out) {
    switch (value) {
      case JsonValue.JsonString s -> quote(s.value(), out);
      case JsonValue.JsonNumber n -> out.append(n.value());
      case JsonValue.JsonBool b -> out.append(b.value() ? "true" : "false");
      case JsonValue.JsonNull n -> out.append("null");
      case JsonValue.JsonArray a -> array(a.items(), depth, out);
      case JsonObject o -> object(o.members(), depth, out);
    }
  }

  private static void array(List<JsonValue> items, int depth, StringBuilder out) {
    if (items.isEmpty()) {
      out.append("[]");
      return;
    }
    out.append("[\n");
    for (int i = 0; i < items.size(); i++) {
      indent(depth + 1, out);
      write(items.get(i), depth + 1, out);
      out.append(i < items.size() - 1 ? ",\n" : "\n");
    }
    indent(depth, out);
    out.append(']');
  }

  private static void object(List<JsonObject.Member> members, int depth, StringBuilder out) {
    if (members.isEmpty()) {
      out.append("{}");
      return;
    }
    out.append("{\n");
    for (int i = 0; i < members.size(); i++) {
      JsonObject.Member member = members.get(i);
      indent(depth + 1, out);
      quote(member.name(), out);
      out.append(": ");
      write(member.value(), depth + 1, out);
      out.append(i < members.size() - 1 ? ",\n" : "\n");
    }
    indent(depth, out);
    out.append('}');
  }

  private static void indent(int depth, StringBuilder out) {
    for (int i = 0; i < depth; i++) {
      out.append("  ");
    }
  }

  private static void quote(String text, StringBuilder out) {
    out.append('"');
    for (int i = 0; i < text.length(); i++) {
      char c = text.charAt(i);
      switch (c) {
        case '"' -> out.append("\\\"");
        case '\\' -> out.append("\\\\");
        case '\n' -> out.append("\\n");
        case '\r' -> out.append("\\r");
        case '\t' -> out.append("\\t");
        default -> {
          if (c < 0x20) {
            out.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c));
          } else {
            out.append(c);
          }
        }
      }
    }
    out.append('"');
  }
}
