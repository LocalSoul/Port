package io.github.localsoul.port.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.googlejavaformat.java.Formatter;
import com.google.googlejavaformat.java.FormatterException;
import io.github.localsoul.port.ir.IrSyntaxException;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * Golden Files des Generators ({@code testdata/golden/java/}): Java aus den IR-Fixtures von M2 mit
 * dem Mapping des Unity-Adapters. Aktualisieren nur mit {@code -Dgolden.update=true}.
 */
class JavaGeneratorGoldenTest {

  private static GenerationResult result;

  @BeforeAll
  static void generate() throws IOException, IrSyntaxException, MappingException {
    result = new JavaGenerator(GoldenFixtures.config()).generate(GoldenFixtures.irFiles());
  }

  /** Todo-Liste als Text, eine Zeile je Eintrag. */
  private static String todoReport() {
    StringBuilder out = new StringBuilder();
    for (Todo todo : result.todos()) {
      out.append(todo.symbol()).append('\t').append(todo.reason()).append('\n');
    }
    return out.toString();
  }

  @Test
  void matchesGoldenFiles() throws IOException {
    boolean update = Boolean.getBoolean("golden.update");
    List<String> mismatches = new ArrayList<>();
    for (GeneratedFile file : result.files()) {
      Path golden = GoldenFixtures.JAVA_GOLDEN.resolve(file.path());
      if (update) {
        Files.createDirectories(golden.getParent());
        Files.writeString(golden, file.content(), StandardCharsets.UTF_8);
      }
      if (!Files.exists(golden)
          || !Files.readString(golden, StandardCharsets.UTF_8).equals(file.content())) {
        mismatches.add(file.path());
      }
    }
    Path todos = GoldenFixtures.JAVA_GOLDEN.resolve("todos.tsv");
    if (update) {
      Files.writeString(todos, todoReport(), StandardCharsets.UTF_8);
    }
    assertEquals(List.of(), mismatches, "abweichend, mit -Dgolden.update=true neu erzeugen");
    assertEquals(Files.readString(todos, StandardCharsets.UTF_8), todoReport());
  }

  @Test
  void outputIsGoogleJavaFormatConform() throws FormatterException {
    Formatter formatter = new Formatter();
    List<String> notConform = new ArrayList<>();
    for (GeneratedFile file : result.files()) {
      String formatted = formatter.formatSource(file.content());
      if (!formatted.equals(file.content())) {
        notConform.add(file.path() + "\n--- erwartet (google-java-format) ---\n" + formatted);
      }
    }
    assertEquals(List.of(), notConform);
  }

  @Test
  void isDeterministic() throws IOException, IrSyntaxException, MappingException {
    GenerationResult again =
        new JavaGenerator(GoldenFixtures.config()).generate(GoldenFixtures.irFiles());
    assertEquals(result, again);
  }

  @Test
  void moverProducesBaseAndThinClass() {
    List<String> paths = new ArrayList<>();
    for (GeneratedFile file : result.files()) {
      paths.add(file.path());
    }
    assertTrue(paths.contains("gen/fixtures/MoverBase.java"), paths.toString());
    assertTrue(paths.contains("gen/fixtures/Mover.java"), paths.toString());
  }
}
