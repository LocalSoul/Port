package io.github.localsoul.port.engine.core.event;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.subsystem.Event;
import io.github.localsoul.port.engine.api.subsystem.EventType;
import io.github.localsoul.port.engine.api.subsystem.SubsystemConfigurationException;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystem.TestSubsystem;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

class EventBusTest {

  private static final EventType TICK = new EventType("test.tick", 1);
  private static final EventType OTHER = new EventType("test.other", 1);

  /** Ergebnis eines Laufs: pro Phasenschritt veröffentlichte und gesehene Nutzdaten. */
  private record Run(List<List<String>> published, List<List<String>> seen) {}

  private static Run run(int frames) {
    List<List<String>> published = new ArrayList<>();
    List<List<String>> seen = new ArrayList<>();
    TestSubsystem[] holder = new TestSubsystem[2];
    // Konsument läuft vor dem Produzenten: er sieht trotzdem nur den vorigen Schritt.
    holder[0] =
        TestSubsystem.withEvents(
            "consumer",
            Set.of(),
            Set.of(TICK),
            ctx -> {
              List<String> payloads = new ArrayList<>();
              for (Event event : holder[0].bus().events(TICK)) {
                payloads.add(event.sequence() + ":" + event.payload());
              }
              seen.add(payloads);
            });
    holder[1] =
        TestSubsystem.withEvents(
            "producer",
            Set.of(TICK),
            Set.of(),
            ctx -> {
              List<String> payloads = new ArrayList<>();
              int count = (int) (ctx.time().frame() % 3) + 1;
              for (int i = 0; i < count; i++) {
                String payload = label(ctx) + "#" + i;
                holder[1].bus().publish(TICK, payload);
                payloads.add(payload);
              }
              published.add(payloads);
            });
    CoreEngine engine =
        CoreEngine.builder()
            .fixedDelta(0.02f)
            .frameDelta(0.025f)
            .subsystem(holder[0])
            .subsystem(holder[1])
            .build();
    engine.step(frames);
    return new Run(published, seen);
  }

  private static String label(FrameContext ctx) {
    return ctx.time().frame() + "/" + ctx.phase();
  }

  @Test
  void eventsAreVisibleExactlyInNextPhaseStepInPublicationOrder() {
    Run run = run(1000);
    assertTrue(run.seen().get(0).isEmpty());
    for (int step = 1; step < run.seen().size(); step++) {
      List<String> expected = run.published().get(step - 1);
      List<String> actual = new ArrayList<>();
      for (String entry : run.seen().get(step)) {
        actual.add(entry.substring(entry.indexOf(':') + 1));
      }
      assertEquals(expected, actual, "Schritt " + step);
    }
  }

  @Test
  void sequenceNumbersAreStrictlyIncreasing() {
    long previous = -1;
    for (List<String> step : run(200).seen()) {
      for (String entry : step) {
        long sequence = Long.parseLong(entry.substring(0, entry.indexOf(':')));
        assertTrue(sequence > previous);
        previous = sequence;
      }
    }
  }

  @Test
  void runsAreDeterministicOver1000Frames() {
    assertEquals(run(1000), run(1000));
  }

  @Test
  void subsystemBusRejectsUndeclaredTypes() {
    TestSubsystem subsystem = TestSubsystem.withEvents("s", Set.of(TICK), Set.of(TICK), ctx -> {});
    CoreEngine.builder().subsystem(subsystem).build();
    SubsystemConfigurationException publish =
        assertThrows(
            SubsystemConfigurationException.class, () -> subsystem.bus().publish(OTHER, 1));
    assertEquals(
        "Subsystem 's' veröffentlicht test.other@1, hat es aber nicht deklariert",
        publish.getMessage());
    assertThrows(SubsystemConfigurationException.class, () -> subsystem.bus().events(OTHER));
  }

  @Test
  void adapterCallbacksUseUncheckedBus() {
    CoreEngine engine = CoreEngine.builder().build();
    List<Integer> counts = new ArrayList<>();
    engine.register(
        io.github.localsoul.port.engine.api.schedule.Phase.EARLY,
        ctx -> ctx.events().publish(OTHER, "x"));
    engine.register(
        io.github.localsoul.port.engine.api.schedule.Phase.FIXED,
        ctx -> counts.add(ctx.events().events(OTHER).size()));
    engine.register(
        io.github.localsoul.port.engine.api.schedule.Phase.UPDATE,
        ctx -> counts.add(ctx.events().events(OTHER).size()));
    engine.step(1);
    // Frame 0 mit 1/60 s hat keinen FIXED-Schritt: UPDATE ist der nächste Schritt nach EARLY.
    assertEquals(List.of(1), counts);
  }
}
