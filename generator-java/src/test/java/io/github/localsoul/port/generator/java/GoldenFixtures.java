package io.github.localsoul.port.generator.java;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.reader.IrReader;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * Eingaben der Generator-Tests: IR-Golden-Files aus M2 und die Mappings des Unity-Adapters und von
 * {@code runtime-system}.
 */
final class GoldenFixtures {

  static final Path ROOT = Path.of("..").toAbsolutePath().normalize();
  static final Path IR_DIR = ROOT.resolve("testdata/golden/ir/unity");
  static final Path JAVA_GOLDEN = ROOT.resolve("testdata/golden/java");
  static final Path UNITY_MAPPING =
      ROOT.resolve("engines/unity/adapter-unity/src/main/resources/mapping.tsv");
  static final Path RUNTIME_MAPPING = ROOT.resolve("runtime-system/src/main/resources/mapping.tsv");
  static final String PACKAGE = "gen.fixtures";

  private GoldenFixtures() {}

  static List<IrFile> irFiles() throws IOException, IrSyntaxException {
    List<Path> paths = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(IR_DIR, "*.ir")) {
      for (Path path : stream) {
        paths.add(path);
      }
    }
    paths.sort(null);
    List<IrFile> files = new ArrayList<>();
    IrReader reader = new IrReader();
    for (Path path : paths) {
      files.add(
          reader.read(
              Files.readString(path, StandardCharsets.UTF_8), path.getFileName().toString()));
    }
    return files;
  }

  static GeneratorConfig config() throws IOException, MappingException {
    Mapping mapping = Mapping.load(UNITY_MAPPING).merge(Mapping.load(RUNTIME_MAPPING));
    return new GeneratorConfig(PACKAGE, mapping, java.util.Set.of());
  }
}
