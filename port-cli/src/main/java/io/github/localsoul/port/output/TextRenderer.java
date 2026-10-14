package io.github.localsoul.port.output;

import io.github.localsoul.port.core.CommandResult;
import io.github.localsoul.port.core.Diagnostic;
import io.github.localsoul.port.core.ExitCode;
import io.github.localsoul.port.core.Severity;
import io.github.localsoul.port.json.JsonObject;
import io.github.localsoul.port.json.JsonValue;
import java.io.PrintStream;

/**
 * Menschenlesbare Ausgabe: Daten als {@code schlüssel: wert} auf stdout, Diagnosen als {@code stufe
 * ID ort: meldung} auf stderr. Mit {@code -q} nur Fehler.
 */
public final class TextRenderer {

  private TextRenderer() {}

  /**
   * Gibt ein Ergebnis aus.
   *
   * @param result Ergebnis
   * @param exitCode abgeleiteter Exit-Code
   * @param quiet {@code -q}
   * @param out stdout
   * @param err stderr
   */
  public static void render(
      CommandResult result, ExitCode exitCode, boolean quiet, PrintStream out, PrintStream err) {
    for (Diagnostic diagnostic : result.diagnostics()) {
      if (!quiet || diagnostic.severity() == Severity.ERROR) {
        err.println(line(diagnostic));
      }
    }
    if (quiet) {
      return;
    }
    for (JsonObject.Member member : result.data().members()) {
      if (member.value() instanceof JsonValue.JsonArray array && !array.items().isEmpty()) {
        out.println(member.name() + ":");
        for (JsonValue item : array.items()) {
          out.println("  " + text(item));
        }
      } else {
        out.println(member.name() + ": " + text(member.value()));
      }
    }
    out.println(
        "port "
            + result.command()
            + ": "
            + (exitCode == ExitCode.SUCCESS ? "ok" : "fehlgeschlagen")
            + " (Exit "
            + exitCode.code()
            + ")");
  }

  private static String line(Diagnostic diagnostic) {
    StringBuilder text = new StringBuilder(diagnostic.severity().spelling());
    text.append(' ').append(diagnostic.id());
    if (diagnostic.location() != null) {
      text.append(' ').append(diagnostic.location());
    }
    if (diagnostic.symbol() != null) {
      text.append(' ').append(diagnostic.symbol());
    }
    text.append(": ").append(diagnostic.message());
    if (diagnostic.hint() != null) {
      text.append(" (").append(diagnostic.hint()).append(')');
    }
    return text.toString();
  }

  /** Objekt einzeilig: Werte durch zwei Leerzeichen getrennt (Listen-Einträge). */
  private static String line(JsonObject object) {
    StringBuilder out = new StringBuilder();
    for (JsonObject.Member member : object.members()) {
      if (!out.isEmpty()) {
        out.append("  ");
      }
      out.append(text(member.value()));
    }
    return out.toString();
  }

  private static String text(JsonValue value) {
    return switch (value) {
      case JsonValue.JsonString s -> s.value();
      case JsonValue.JsonNumber n -> Long.toString(n.value());
      case JsonValue.JsonBool b -> Boolean.toString(b.value());
      case JsonValue.JsonNull n -> "-";
      case JsonValue.JsonArray a -> a.items().isEmpty() ? "-" : a.items().size() + " Einträge";
      case JsonObject o -> line(o);
    };
  }
}
