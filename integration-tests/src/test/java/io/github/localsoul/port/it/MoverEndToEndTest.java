package io.github.localsoul.port.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.adapter.unity.GameObject;
import io.github.localsoul.adapter.unity.MonoBehaviour;
import io.github.localsoul.adapter.unity.UnityRuntime;
import io.github.localsoul.adapter.unity.Vector3;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystems.input.InputScript;
import io.github.localsoul.port.engine.core.subsystems.input.ScriptedInput;
import io.github.localsoul.port.generator.java.GeneratedFile;
import io.github.localsoul.port.generator.java.GenerationResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * M3.3: IR des {@code Mover} (Golden File aus M2) → Java → kompiliert → läuft headless gegen {@code
 * adapter-unity} und {@code engine-core}.
 */
class MoverEndToEndTest {

  private static final float FIXED_DELTA = 0.02f;
  private static final float FRAME_DELTA = 1f / 60f;
  private static final int FRAMES = 60;

  private static GenerationResult generated;
  private static ClassLoader classes;

  @BeforeAll
  static void generateAndCompile() throws Exception {
    generated = GeneratedFixtures.generated();
    classes = GeneratedFixtures.classes();
  }

  /** Ein Lauf: GameObject mit Mover, Horizontal = 1 ab Frame 0, 60 Frames. */
  private static Run run() throws Exception {
    CoreEngine engine =
        CoreEngine.builder()
            .fixedDelta(FIXED_DELTA)
            .frameDelta(FRAME_DELTA)
            .subsystem(new ScriptedInput(new InputScript().at(0).axis("Horizontal", 1f)))
            .build();
    UnityRuntime.install(engine);
    GameObject player = new GameObject("player");
    MonoBehaviour mover =
        (MonoBehaviour)
            classes.loadClass("gen.fixtures.Mover").getDeclaredConstructor().newInstance();
    player.attachScript(mover);
    engine.step(FRAMES);
    return new Run(player.transform().position(), engine.world().dump());
  }

  private record Run(Vector3 position, String dump) {}

  @Test
  void moverMovesBySpeedTimesSumOfDeltaTime() throws Exception {
    Run run = run();
    // Erwartung unabhängig berechnet: je Frame (axis * speed) * deltaTime, axis = 1 (Rohwert,
    // GetAxis ist stub), speed = 5 laut Mover.cs, deltaTime = Frame-Dauer; float wie im Spiel.
    float speed = 5.0f;
    float x = 0f;
    for (int frame = 0; frame < FRAMES; frame++) {
      x += (1f * speed) * FRAME_DELTA;
    }
    assertEquals(new Vector3(x, 0f, 0f), run.position());
    assertEquals(5.0f, run.position().x(), 1e-4f, "60 Frames à 1/60 s mit 5 m/s ≈ 5 m");
  }

  @Test
  void runIsDeterministic() throws Exception {
    assertEquals(run(), run());
  }

  @Test
  void generatedCodeImportsOnlyTheAdapterAndRuntimeSystem() {
    for (GeneratedFile file : generated.files()) {
      for (String line : file.content().split("\n")) {
        if (line.startsWith("import ")) {
          assertTrue(
              line.startsWith("import io.github.localsoul.adapter.unity.")
                  || line.startsWith("import io.github.localsoul.runtime.system."),
              file.path() + ": " + line);
        }
      }
    }
  }

  @Test
  void allGeneratedFixturesCompileAndManualWorkIsUsed() throws Exception {
    Class<?> unsafeBits = classes.loadClass("gen.fixtures.UnsafeBits");
    Object instance = unsafeBits.getDeclaredConstructor().newInstance();
    int result =
        (int)
            unsafeBits.getMethod("first", int[].class).invoke(instance, (Object) new int[] {4, 9});
    assertEquals(4 + (4 + 1), result);
    Class<?> controlFlow = classes.loadClass("gen.fixtures.ControlFlow");
    Object flow = controlFlow.getDeclaredConstructor().newInstance();
    assertEquals("klein", controlFlow.getMethod("describe", int.class).invoke(flow, 2));
    assertEquals(
        6, controlFlow.getMethod("sum", int[].class).invoke(flow, (Object) new int[] {1, -3, 5}));
    assertEquals(
        2.0f,
        controlFlow
            .getMethod("average", float[].class)
            .invoke(flow, (Object) new float[] {1f, 3f}));
  }
}
