package io.github.localsoul.launcher.manifest;

import java.util.List;

/** Schreibt einfaches, deterministisches TOML (Tabellen, Texte, Textlisten, Zahlen). */
public final class TomlText {

  private final StringBuilder out = new StringBuilder();

  /**
   * Beginnt eine Tabelle; vor jeder außer der ersten steht eine Leerzeile.
   *
   * @param name Tabellenname, z. B. {@code game.requires}
   * @return dieser Schreiber
   */
  public TomlText table(String name) {
    if (!out.isEmpty()) {
      out.append('\n');
    }
    out.append('[').append(name).append("]\n");
    return this;
  }

  /**
   * Schreibt {@code key = "wert"}.
   *
   * @param key Schlüssel
   * @param value Text
   * @return dieser Schreiber
   */
  public TomlText entry(String key, String value) {
    out.append(key).append(" = ").append(quote(value)).append('\n');
    return this;
  }

  /**
   * Schreibt {@code key = zahl}.
   *
   * @param key Schlüssel
   * @param value Zahl
   * @return dieser Schreiber
   */
  public TomlText entry(String key, long value) {
    out.append(key).append(" = ").append(value).append('\n');
    return this;
  }

  /**
   * Schreibt {@code key = ["a", "b"]}.
   *
   * @param key Schlüssel
   * @param values Texte
   * @return dieser Schreiber
   */
  public TomlText entry(String key, List<String> values) {
    out.append(key).append(" = [");
    for (int i = 0; i < values.size(); i++) {
      out.append(i == 0 ? "" : ", ").append(quote(values.get(i)));
    }
    out.append("]\n");
    return this;
  }

  /**
   * Ergebnis.
   *
   * @return TOML-Text
   */
  public String text() {
    return out.toString();
  }

  private static String quote(String value) {
    return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
  }
}
