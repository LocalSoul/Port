package io.github.localsoul.port.engine.core.subsystem;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.localsoul.port.engine.api.component.Schemas;
import io.github.localsoul.port.engine.api.subsystem.SubsystemConfigurationException;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystems.input.InputScript;
import io.github.localsoul.port.engine.core.subsystems.input.ScriptedInput;
import io.github.localsoul.port.engine.core.subsystems.render.NullRenderer;
import java.util.Set;
import org.junit.jupiter.api.Test;

class SubsystemRegistryTest {

  private static String failure(CoreEngine.Builder builder) {
    return assertThrows(SubsystemConfigurationException.class, builder::build).getMessage();
  }

  @Test
  void rejectsTwoWritersOfSameComponent() {
    String message =
        failure(
            CoreEngine.builder()
                .subsystem(new ScriptedInput(new InputScript()))
                .subsystem(
                    TestSubsystem.declaring(
                        "input-other", "input2", Set.of(Schemas.INPUT_STATE), Set.of(), Set.of())));
    assertEquals(
        "Komponente InputState@1 wird von 'input-scripted' und 'input-other' geschrieben;"
            + " erlaubt ist genau ein Schreiber",
        message);
  }

  @Test
  void rejectsWritersOfCoreOwnedComponents() {
    String message =
        failure(
            CoreEngine.builder()
                .subsystem(
                    TestSubsystem.declaring(
                        "physics-x",
                        "physics",
                        Set.of(Schemas.WORLD_TRANSFORM),
                        Set.of(),
                        Set.of())));
    assertEquals(
        "Komponente WorldTransform@1 wird von 'core' und 'physics-x' geschrieben;"
            + " erlaubt ist genau ein Schreiber",
        message);
  }

  @Test
  void rejectsUnmetRequirement() {
    String message =
        failure(
            CoreEngine.builder()
                .subsystem(new NullRenderer())
                .subsystem(
                    TestSubsystem.declaring(
                        "audio-x", "audio", Set.of(), Set.of("physics"), Set.of())));
    assertEquals(
        "Subsystem 'audio-x' braucht 'physics', aber kein anderes Subsystem hat diese ID oder Art",
        message);
  }

  @Test
  void acceptsRequirementByKindOrId() {
    assertDoesNotThrow(
        () ->
            CoreEngine.builder()
                .subsystem(new NullRenderer())
                .subsystem(new ScriptedInput(new InputScript()))
                .subsystem(
                    TestSubsystem.declaring(
                        "a", "audio", Set.of(), Set.of("render", "input-scripted"), Set.of()))
                .build());
  }

  @Test
  void requirementCannotBeSatisfiedBySelf() {
    String message =
        failure(
            CoreEngine.builder()
                .subsystem(
                    TestSubsystem.declaring("a", "audio", Set.of(), Set.of("audio"), Set.of())));
    assertEquals(
        "Subsystem 'a' braucht 'audio', aber kein anderes Subsystem hat diese ID oder Art",
        message);
  }

  @Test
  void rejectsConflicts() {
    String message =
        failure(
            CoreEngine.builder()
                .subsystem(new NullRenderer())
                .subsystem(
                    TestSubsystem.declaring(
                        "vr", "render2", Set.of(), Set.of(), Set.of("render"))));
    assertEquals(
        "Subsystem 'vr' verträgt sich nicht mit 'render-null' (conflictsWith 'render')", message);
  }

  @Test
  void rejectsDuplicateIds() {
    String message =
        failure(CoreEngine.builder().subsystem(new NullRenderer()).subsystem(new NullRenderer()));
    assertEquals("Subsystem-ID 'render-null' ist doppelt registriert", message);
  }

  @Test
  void reportsFirstViolationDeterministically() {
    String first = null;
    for (int i = 0; i < 20; i++) {
      String message =
          failure(
              CoreEngine.builder()
                  .subsystem(
                      TestSubsystem.declaring(
                          "a", "x", Set.of(), Set.of("zeta", "alpha", "mid"), Set.of())));
      if (first == null) {
        first = message;
      }
      assertEquals(first, message);
    }
    assertEquals(
        "Subsystem 'a' braucht 'alpha', aber kein anderes Subsystem hat diese ID oder Art", first);
  }
}
