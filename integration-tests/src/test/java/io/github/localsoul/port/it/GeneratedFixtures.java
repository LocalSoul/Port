package io.github.localsoul.port.it;

import io.github.localsoul.port.generator.java.GeneratedFile;
import io.github.localsoul.port.generator.java.GenerationResult;
import io.github.localsoul.port.generator.java.GeneratorConfig;
import io.github.localsoul.port.generator.java.JavaGenerator;
import io.github.localsoul.port.generator.java.Mapping;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.reader.IrReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Alle IR-Golden-Files aus M2 → Java (Mappings von {@code adapter-unity} und {@code
 * runtime-system}) → im Speicher kompiliert; einmal je Testlauf.
 *
 * <p>Die Mappings werden aus dem Repository gelesen, nicht als Klassenpfad-Ressource: beide Module
 * liefern {@code /mapping.tsv}, auf dem Klassenpfad wäre nur eine sichtbar.
 */
final class GeneratedFixtures {

  static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
  private static final Path IR_DIR = ROOT.resolve("testdata/golden/ir/unity");
  private static final Path MANUAL = Path.of("src/test/resources/manual");
  private static final Path UNITY_MAPPING =
      ROOT.resolve("engines/unity/adapter-unity/src/main/resources/mapping.tsv");
  private static final Path RUNTIME_MAPPING =
      ROOT.resolve("runtime-system/src/main/resources/mapping.tsv");

  private static GenerationResult generated;
  private static ClassLoader classes;

  private GeneratedFixtures() {}

  /** Ergebnis des Generators. */
  static synchronized GenerationResult generated() throws Exception {
    ensureCompiled();
    return generated;
  }

  /** Klassenlader mit allen erzeugten Klassen und {@code manual/UnsafeBits}. */
  static synchronized ClassLoader classes() throws Exception {
    ensureCompiled();
    return classes;
  }

  private static void ensureCompiled() throws Exception {
    if (classes != null) {
      return;
    }
    Mapping mapping = Mapping.load(UNITY_MAPPING).merge(Mapping.load(RUNTIME_MAPPING));
    GeneratorConfig config = new GeneratorConfig("gen.fixtures", mapping, Set.of("UnsafeBits"));
    generated = new JavaGenerator(config).generate(readIr());
    Map<String, String> sources = new TreeMap<>();
    for (GeneratedFile file : generated.files()) {
      sources.put(file.path(), file.content());
    }
    sources.put(
        "gen/fixtures/UnsafeBits.java",
        Files.readString(MANUAL.resolve("gen/fixtures/UnsafeBits.java")));
    classes = InMemoryCompiler.compile(sources);
  }

  private static List<IrFile> readIr() throws Exception {
    List<Path> paths = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(IR_DIR, "*.ir")) {
      for (Path path : stream) {
        paths.add(path);
      }
    }
    paths.sort(null);
    List<IrFile> files = new ArrayList<>();
    for (Path path : paths) {
      files.add(
          new IrReader()
              .read(Files.readString(path, StandardCharsets.UTF_8), path.getFileName().toString()));
    }
    return files;
  }
}
