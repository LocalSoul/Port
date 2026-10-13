package io.github.localsoul.port.frontend.unity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Wrapper ohne .NET: Rohausgabe kommt aus {@code src/test/resources/raw}. */
class UnityFrontendTest {

  private static final FrontendConfig CONFIG =
      new FrontendConfig(List.of("port-frontend-unity"), Duration.ofSeconds(5));
  private static final Path ASSEMBLY = Path.of("Assembly-CSharp.dll");

  private static UnityFrontend frontend(FakeToolRunner runner) {
    return new UnityFrontend(CONFIG, runner);
  }

  @Test
  void canonicalizesRawOutput() throws FrontendException {
    FakeToolRunner runner = new FakeToolRunner(List.of("Sample.ir"), 0, "1 Typen");
    TranslationResult result = frontend(runner).translate(TranslationRequest.of(ASSEMBLY));
    String expected =
        "(ir :version 1 :assembly \"Fixture\")\n"
            + "\n"
            + "(class Sample :mods (public)\n"
            + "  (field a i32 :mods (private))\n"
            + "  (method B void () (block)))\n";
    assertEquals(expected, result.canonicalTexts().get("Sample"));
    assertEquals("Sample", result.files().get("Sample").types().get(0).name());
    assertEquals("1 Typen", result.summary());
  }

  @Test
  void buildsCommandLineFromRequest() throws FrontendException {
    FakeToolRunner runner = new FakeToolRunner(List.of(), 0, "");
    TranslationRequest request =
        new TranslationRequest(
            ASSEMBLY, List.of("Mover", "Game.*"), List.of(Path.of("refs")), false);
    frontend(runner).translate(request);
    List<String> command = runner.commands.get(0);
    assertEquals("port-frontend-unity", command.get(0));
    assertEquals(List.of("--assembly", "Assembly-CSharp.dll"), command.subList(1, 3));
    assertEquals("--out", command.get(3));
    assertEquals(
        List.of("--classes", "Mover", "--classes", "Game.*", "--references", "refs", "--no-sha256"),
        command.subList(5, command.size()));
  }

  @Test
  void mapsExitCode3ToInputNotFound() {
    FakeToolRunner runner = new FakeToolRunner(List.of(), 3, "Assembly nicht gefunden: x.dll");
    FrontendException e =
        assertThrows(
            FrontendException.class,
            () -> frontend(runner).translate(TranslationRequest.of(ASSEMBLY)));
    assertEquals(FrontendException.INPUT_NOT_FOUND, e.exitCode());
    assertTrue(e.getMessage().contains("Assembly nicht gefunden"), e.getMessage());
  }

  @Test
  void mapsOtherExitCodesToTranslatorFailure() {
    for (int code : new int[] {1, 2, 4, 134}) {
      FakeToolRunner runner = new FakeToolRunner(List.of(), code, "Absturz");
      FrontendException e =
          assertThrows(
              FrontendException.class,
              () -> frontend(runner).translate(TranslationRequest.of(ASSEMBLY)));
      assertEquals(FrontendException.TRANSLATOR_FAILURE, e.exitCode(), "Exit " + code);
    }
  }

  @Test
  void invalidOutputReportsPosition() {
    FakeToolRunner runner = new FakeToolRunner(List.of("Sample.ir", "Bad.ir"), 0, "");
    FrontendException e =
        assertThrows(
            FrontendException.class,
            () -> frontend(runner).translate(TranslationRequest.of(ASSEMBLY)));
    assertEquals(FrontendException.TRANSLATOR_FAILURE, e.exitCode());
    assertTrue(e.getMessage().contains("Bad.ir:4:12"), e.getMessage());
    assertTrue(e.getMessage().contains("ohne Typsuffix"), e.getMessage());
  }

  @Test
  void missingProgramIsGeneralError() {
    UnityFrontend frontend =
        new UnityFrontend(
            new FrontendConfig(
                List.of("/gibt/es/nicht/port-frontend-unity"), Duration.ofSeconds(5)),
            new ProcessToolRunner());
    FrontendException e =
        assertThrows(
            FrontendException.class, () -> frontend.translate(TranslationRequest.of(ASSEMBLY)));
    assertEquals(FrontendException.GENERAL, e.exitCode());
    assertTrue(e.getMessage().contains("nicht gefunden"), e.getMessage());
  }

  @Test
  void configStartsDllsWithDotnet() {
    assertEquals(
        List.of("dotnet", "/x/port-frontend-unity.dll"),
        FrontendConfig.forProgram("/x/port-frontend-unity.dll").commandPrefix());
    assertEquals(List.of("tool"), FrontendConfig.forProgram("tool").commandPrefix());
  }
}
