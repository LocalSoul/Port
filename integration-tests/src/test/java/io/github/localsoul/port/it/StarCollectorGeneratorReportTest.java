package io.github.localsoul.port.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.localsoul.port.frontend.unity.FrontendConfig;
import io.github.localsoul.port.frontend.unity.ProcessToolRunner;
import io.github.localsoul.port.frontend.unity.TranslationRequest;
import io.github.localsoul.port.frontend.unity.TranslationResult;
import io.github.localsoul.port.frontend.unity.UnityFrontend;
import io.github.localsoul.port.generator.java.GenerationResult;
import io.github.localsoul.port.generator.java.GeneratorConfig;
import io.github.localsoul.port.generator.java.JavaGenerator;
import io.github.localsoul.port.generator.java.Mapping;
import io.github.localsoul.port.generator.java.Todo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * StarCollector (eigenes Spiel des Nutzers, {@code testdata/starcollector}) durch Frontend und
 * Generator; Bericht der offenen Stellen in {@code target/starcollector-generator-report.txt}.
 * Abnahme M3a: keine Lücken mehr aus der .NET-Basisbibliothek ({@code System.*}), nur noch
 * Unity-Adapter-Lücken. Ohne gebautes .NET-Frontend übersprungen ({@code scripts/build-dotnet.sh}).
 */
class StarCollectorGeneratorReportTest {

  private static final Path ROOT = GeneratedFixtures.ROOT;
  private static final Path TOOL_DLL =
      ROOT.resolve(
          "frontend-unity/dotnet/src/PortFrontendUnity/bin/Release/net8.0/port-frontend-unity.dll");
  private static final Path ASSEMBLY = ROOT.resolve("testdata/starcollector/Assembly-CSharp.dll");
  private static final Path UNITY_DLLS =
      ROOT.resolve("testdata/Builds/Linux/StarCollector_Data/Managed");
  private static final Path REPORT = Path.of("target", "starcollector-generator-report.txt");

  @Test
  void generatesGameWithoutBclGaps() throws Exception {
    assumeTrue(Files.exists(TOOL_DLL), "port-frontend-unity nicht gebaut: scripts/build-dotnet.sh");
    List<Path> references = Files.isDirectory(UNITY_DLLS) ? List.of(UNITY_DLLS) : List.of();
    UnityFrontend frontend =
        new UnityFrontend(FrontendConfig.forDll(TOOL_DLL), new ProcessToolRunner());
    TranslationResult translated =
        frontend.translate(new TranslationRequest(ASSEMBLY, List.of(), references, true));
    Mapping mapping =
        Mapping.load(ROOT.resolve("engines/unity/adapter-unity/src/main/resources/mapping.tsv"))
            .merge(Mapping.load(ROOT.resolve("runtime-system/src/main/resources/mapping.tsv")));
    GeneratorConfig config = new GeneratorConfig("gen.starcollector", mapping, Set.of());
    GenerationResult generated =
        new JavaGenerator(config).generate(new ArrayList<>(translated.files().values()));

    List<String> bclGaps = new ArrayList<>();
    StringBuilder report = new StringBuilder("StarCollector: offene Stellen des Generators\n");
    report.append("Unity-DLLs: ").append(references.isEmpty() ? "nein" : "ja").append('\n');
    report.append("Dateien: ").append(generated.files().size()).append('\n');
    report.append("Todos: ").append(generated.todos().size()).append('\n');
    for (Todo todo : generated.todos()) {
      report.append("  ").append(todo.symbol()).append('\t').append(todo.reason()).append('\n');
      if (todo.reason().contains(" System.")) {
        bclGaps.add(todo.symbol() + ": " + todo.reason());
      }
    }
    Files.createDirectories(REPORT.getParent());
    Files.writeString(REPORT, report.toString(), StandardCharsets.UTF_8);
    assertEquals(List.of(), bclGaps, "BCL-Lücken, Bericht: " + REPORT.toAbsolutePath());
  }
}
