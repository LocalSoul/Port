package io.github.localsoul.port.json;

import java.util.List;

/**
 * Unveränderlicher JSON-Wert für die Ausgabe. Objekte behalten die Reihenfolge ihrer Schlüssel
 * (Vertrag: feste, dokumentierte Reihenfolge), Listen werden vom Aufrufer sortiert übergeben.
 */
public sealed interface JsonValue
    permits JsonValue.JsonString,
        JsonValue.JsonNumber,
        JsonValue.JsonBool,
        JsonValue.JsonNull,
        JsonValue.JsonArray,
        JsonObject {

  /**
   * Zeichenkette.
   *
   * @param value Inhalt
   */
  record JsonString(String value) implements JsonValue {}

  /**
   * Ganzzahl.
   *
   * @param value Wert
   */
  record JsonNumber(long value) implements JsonValue {}

  /**
   * Wahrheitswert.
   *
   * @param value Wert
   */
  record JsonBool(boolean value) implements JsonValue {}

  /** {@code null}. */
  record JsonNull() implements JsonValue {}

  /**
   * Liste.
   *
   * @param items Elemente in Ausgabereihenfolge
   */
  record JsonArray(List<JsonValue> items) implements JsonValue {

    /** Kopiert die Elemente. */
    public JsonArray {
      items = List.copyOf(items);
    }
  }

  /**
   * Text oder {@code null}.
   *
   * @param value Text oder {@code null}
   * @return {@link JsonString} oder {@link JsonNull}
   */
  static JsonValue of(String value) {
    return value == null ? new JsonNull() : new JsonString(value);
  }

  /**
   * Liste von Texten.
   *
   * @param values Texte in Ausgabereihenfolge
   * @return Liste
   */
  static JsonArray strings(List<String> values) {
    List<JsonValue> items = new java.util.ArrayList<>();
    for (String value : values) {
      items.add(new JsonString(value));
    }
    return new JsonArray(items);
  }
}
