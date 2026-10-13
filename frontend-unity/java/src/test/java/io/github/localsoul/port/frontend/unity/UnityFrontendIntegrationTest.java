package io.github.localsoul.port.frontend.unity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

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
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Echtes .NET-Programm gegen die Fixtures (M2.2, M2.3). Übersprungen, wenn {@code
 * scripts/build-dotnet.sh} nicht gelaufen ist oder {@code dotnet} fehlt.
 */
class UnityFrontendIntegrationTest {

  private static TranslationResult all;

  @BeforeAll
  static void translateFixtures() throws FrontendException {
    assumeTrue(
        RepoPaths.toolAvailable(), "port-frontend-unity nicht gebaut: scripts/build-dotnet.sh");
    assumeTrue(
        Files.exists(RepoPaths.FIXTURE_DLL), "Fixtures nicht gebaut: scripts/build-dotnet.sh");
    all = translate(List.of());
  }

  private static TranslationResult translate(List<String> classes) throws FrontendException {
    UnityFrontend frontend =
        new UnityFrontend(FrontendConfig.forDll(RepoPaths.TOOL_DLL), new ProcessToolRunner());
    return frontend.translate(
        new TranslationRequest(RepoPaths.FIXTURE_DLL, classes, List.of(), false));
  }

  @ParameterizedTest
  @ValueSource(strings = {"ControlFlow", "Counter", "Holder", "Mover", "UnsafeBits"})
  void matchesGoldenFile(String type) throws IOException {
    String printed = all.canonicalTexts().get(type);
    Path golden = RepoPaths.GOLDEN_DIR.resolve(type + ".ir");
    if (Boolean.getBoolean("golden.update")) {
      Files.createDirectories(RepoPaths.GOLDEN_DIR);
      Files.writeString(golden, printed, StandardCharsets.UTF_8);
    }
    assertTrue(
        Files.exists(golden), "Golden File fehlt, mit -Dgolden.update=true erzeugen: " + golden);
    assertEquals(Files.readString(golden, StandardCharsets.UTF_8), printed);
  }

  @Test
  void translatesExactlyTheFixtureTypes() {
    assertEquals(
        List.of("ControlFlow", "Counter", "Holder", "Mover", "UnsafeBits"),
        new ArrayList<>(all.canonicalTexts().keySet()));
    assertTrue(all.summary().contains("5 Typen"), all.summary());
  }

  @Test
  void outputPassesValidatorWithoutErrors() {
    for (Map.Entry<String, io.github.localsoul.port.ir.ast.IrFile> entry : all.files().entrySet()) {
      List<Diagnostic> errors = new ArrayList<>();
      for (Diagnostic diagnostic : new IrValidator().validate(entry.getValue())) {
        if (diagnostic.severity() == Severity.ERROR) {
          errors.add(diagnostic);
        }
      }
      assertEquals(List.of(), errors, entry.getKey());
    }
  }

  @Test
  void twoRunsGiveIdenticalOutput() throws FrontendException {
    assertEquals(all.canonicalTexts(), translate(List.of()).canonicalTexts());
  }

  @Test
  void classFilterSelectsTypes() throws FrontendException {
    TranslationResult mover = translate(List.of("Mov*"));
    assertEquals(List.of("Mover"), new ArrayList<>(mover.canonicalTexts().keySet()));
  }

  /**
   * Spec Abschnitt 10: gleich bis auf {@code :sha256 "ab12..."} (Golden-Tests ohne Hash) und {@code
   * :mods (public)} an der Klasse (in der Assembly vorhanden, im Spec-Beispiel weggelassen).
   */
  @Test
  void moverMatchesSpecExample() throws IOException {
    String spec =
        Files.readString(
            RepoPaths.ROOT.resolve("testdata/golden/ir/spec-example.ir"), StandardCharsets.UTF_8);
    String expected =
        spec.replace(" :sha256 \"ab12...\"", "")
            .replace(
                "(class Mover :base UnityEngine.MonoBehaviour\n",
                "(class Mover :base UnityEngine.MonoBehaviour :mods (public)\n");
    assertEquals(expected, all.canonicalTexts().get("Mover"));
  }

  @Test
  void unsafeCodeBecomesRawWithManualAndRestIsTranslated() {
    String text = all.canonicalTexts().get("UnsafeBits");
    assertTrue(text.contains("(raw cs \"fixed (int* p = values)"), text);
    assertTrue(text.contains(":manual \"fixed-Block (unsafe, Zeiger)\""), text);
    assertTrue(
        text.contains(
            "  (method Plain i32 ((a i32)) :mods (public)\n    (block\n      (return (+ a 1i32)))))"),
        text);
    assertTrue(all.summary().contains("1 raw-Stellen"), all.summary());
  }
}
