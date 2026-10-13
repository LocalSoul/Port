package io.github.localsoul.port.json;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import org.junit.jupiter.api.Test;

class JsonWriterTest {

  @Test
  void writesObjectsInInsertionOrderWithStableLayout() {
    JsonObject value =
        JsonObject.builder()
            .put("z", 1)
            .put("a", "x")
            .put("n", (String) null)
            .put("b", true)
            .put("list", JsonValue.strings(List.of("p", "q")))
            .put("empty", JsonObject.empty())
            .put("none", new JsonValue.JsonArray(List.of()))
            .build();
    String expected =
        """
        {
          "z": 1,
          "a": "x",
          "n": null,
          "b": true,
          "list": [
            "p",
            "q"
          ],
          "empty": {},
          "none": []
        }
        """;
    assertEquals(expected, JsonWriter.write(value));
  }

  @Test
  void escapesMandatoryCharactersOnly() {
    String text = JsonWriter.write(new JsonValue.JsonString("a\"b\\c\nd\te\u0001ä/"));
    assertEquals("\"a\\\"b\\\\c\\nd\\te\\u0001ä/\"\n", text);
  }

  @Test
  void rejectsDuplicateKeys() {
    JsonObject.Builder builder = JsonObject.builder().put("a", 1);
    assertThrows(IllegalArgumentException.class, () -> builder.put("a", 2));
  }
}
