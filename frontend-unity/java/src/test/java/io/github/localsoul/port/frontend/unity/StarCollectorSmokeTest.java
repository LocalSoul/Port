package io.github.localsoul.port.frontend.unity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.validate.Diagnostic;
import io.github.localsoul.port.ir.validate.IrValidator;
import io.github.localsoul.port.ir.validate.Severity;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import org.junit.jupiter.api.Test;

/**
 * Probelauf mit dem eigenen Spiel des Nutzers (M2.6, {@code testdata/starcollector/README.md}).
 * Kein Golden File: Geprüft wird nur, dass die Übersetzung durchläuft und strukturell gültige IR
 * liefert. Lücken (raw-Stellen, nicht aufgelöste Typen) stehen im Bericht {@code
 * target/starcollector-report.txt}.
 */
class StarCollectorSmokeTest {

  private static final Path ASSEMBLY =
      RepoPaths.ROOT.resolve("testdata/starcollector/Assembly-CSharp.dll");
  private static final Path UNITY_DLLS =
      RepoPaths.ROOT.resolve("testdata/Builds/Linux/StarCollector_Data/Managed");
  private static final Path REPORT = Path.of("target", "starcollector-report.txt");

  /** Strukturfehler, die nie auftreten dürfen; IR002/IR003 sind erwartete Lücken. */
  private static final Set<String> STRUCTURAL = Set.of("IR001", "IR004", "IR005");

  @Test
  void translatesGameAndWritesReport() throws FrontendException, IOException {
    assumeTrue(
        RepoPaths.toolAvailable(), "port-frontend-unity nicht gebaut: scripts/build-dotnet.sh");
    boolean withUnity = Files.isDirectory(UNITY_DLLS);
    List<Path> references = withUnity ? List.of(UNITY_DLLS) : List.of();
    UnityFrontend frontend =
        new UnityFrontend(FrontendConfig.forDll(RepoPaths.TOOL_DLL), new ProcessToolRunner());
    TranslationResult result =
        frontend.translate(new TranslationRequest(ASSEMBLY, List.of(), references, true));

    assertFalse(result.files().isEmpty(), "keine Typen übersetzt");
    Map<String, Integer> diagnosticCounts = new TreeMap<>();
    List<String> structuralErrors = new ArrayList<>();
    StringBuilder details = new StringBuilder();
    for (Map.Entry<String, IrFile> entry : result.files().entrySet()) {
      for (Diagnostic diagnostic : new IrValidator().validate(entry.getValue())) {
        String key = diagnostic.id() + " " + diagnostic.severity().spelling();
        diagnosticCounts.merge(key, 1, Integer::sum);
        details
            .append("  ")
            .append(diagnostic.id())
            .append(' ')
            .append(diagnostic.location())
            .append(' ')
            .append(diagnostic.message())
            .append('\n');
        if (STRUCTURAL.contains(diagnostic.id()) && diagnostic.severity() == Severity.ERROR) {
          structuralErrors.add(diagnostic.toString());
        }
      }
    }
    writeReport(result, withUnity, diagnosticCounts, details.toString());
    assertEquals(List.of(), structuralErrors);
  }

  private static void writeReport(
      TranslationResult result, boolean withUnity, Map<String, Integer> counts, String details)
      throws IOException {
    StringBuilder out = new StringBuilder();
    out.append("StarCollector-Probelauf (M2.6)\n");
    out.append("Unity-DLLs: ")
        .append(withUnity ? "vorhanden (" + UNITY_DLLS + ")" : "fehlen, Unity-Typen unaufgelöst")
        .append("\n\nTypen:\n");
    for (Map.Entry<String, IrFile> entry : result.files().entrySet()) {
      int members = 0;
      for (var type : entry.getValue().types()) {
        members += memberCount(type);
      }
      out.append("  ").append(entry.getKey()).append(": ").append(members).append(" Mitglieder\n");
    }
    out.append("\nAusgabe des Frontends:\n").append(result.summary().strip()).append("\n");
    out.append("\nValidator (ID Schweregrad: Anzahl):\n");
    for (Map.Entry<String, Integer> entry : counts.entrySet()) {
      out.append("  ").append(entry.getKey()).append(": ").append(entry.getValue()).append('\n');
    }
    out.append("\nDiagnosen:\n").append(details);
    Files.createDirectories(REPORT.getParent());
    Files.writeString(REPORT, out.toString(), StandardCharsets.UTF_8);
  }

  private static int memberCount(io.github.localsoul.port.ir.ast.TypeDecl type) {
    return switch (type) {
      case io.github.localsoul.port.ir.ast.ClassDecl c -> c.members().size();
      case io.github.localsoul.port.ir.ast.StructDecl s -> s.members().size();
      case io.github.localsoul.port.ir.ast.InterfaceDecl i -> i.members().size();
      case io.github.localsoul.port.ir.ast.EnumDecl e -> e.members().size();
    };
  }
}
