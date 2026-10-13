package io.github.localsoul.port.json;

import java.util.ArrayList;
import java.util.List;

/**
 * JSON-Objekt mit fester Schlüsselreihenfolge (Einfügereihenfolge).
 *
 * @param members Schlüssel-Wert-Paare in Ausgabereihenfolge
 */
public record JsonObject(List<Member> members) implements JsonValue {

  /** Kopiert die Paare. */
  public JsonObject {
    members = List.copyOf(members);
  }

  /**
   * Schlüssel-Wert-Paar.
   *
   * @param name Schlüssel
   * @param value Wert
   */
  public record Member(String name, JsonValue value) {}

  /**
   * Leeres Objekt.
   *
   * @return {@code {}}
   */
  public static JsonObject empty() {
    return new JsonObject(List.of());
  }

  /**
   * Neuer Builder.
   *
   * @return Builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Sucht den Wert zu einem Schlüssel (für Tests und Renderer).
   *
   * @param name Schlüssel
   * @return Wert oder {@code null}
   */
  public JsonValue get(String name) {
    for (Member member : members) {
      if (member.name().equals(name)) {
        return member.value();
      }
    }
    return null;
  }

  /** Baut ein Objekt in Einfügereihenfolge; doppelte Schlüssel sind ein Programmierfehler. */
  public static final class Builder {

    private final List<Member> members = new ArrayList<>();

    private Builder() {}

    /**
     * Fügt einen Wert an.
     *
     * @param name Schlüssel
     * @param value Wert
     * @return dieser Builder
     * @throws IllegalArgumentException bei doppeltem Schlüssel
     */
    public Builder put(String name, JsonValue value) {
      for (Member member : members) {
        if (member.name().equals(name)) {
          throw new IllegalArgumentException("Schlüssel doppelt: " + name);
        }
      }
      members.add(new Member(name, value));
      return this;
    }

    /**
     * Fügt Text oder {@code null} an.
     *
     * @param name Schlüssel
     * @param value Text oder {@code null}
     * @return dieser Builder
     */
    public Builder put(String name, String value) {
      return put(name, JsonValue.of(value));
    }

    /**
     * Fügt eine Zahl an.
     *
     * @param name Schlüssel
     * @param value Zahl
     * @return dieser Builder
     */
    public Builder put(String name, long value) {
      return put(name, new JsonValue.JsonNumber(value));
    }

    /**
     * Fügt einen Wahrheitswert an.
     *
     * @param name Schlüssel
     * @param value Wert
     * @return dieser Builder
     */
    public Builder put(String name, boolean value) {
      return put(name, new JsonValue.JsonBool(value));
    }

    /**
     * Erzeugt das Objekt.
     *
     * @return Objekt
     */
    public JsonObject build() {
      return new JsonObject(members);
    }
  }
}
