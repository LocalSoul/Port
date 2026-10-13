package io.github.localsoul.port.engine.core.schedule;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.schedule.Time;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SchedulerTest {

  private static final float FIXED = 0.02f;

  @Test
  void runsCallbacksOfAPhaseInRegistrationOrder() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<String> log = new ArrayList<>();
    scheduler.register(Phase.UPDATE, ctx -> log.add("a"));
    scheduler.register(Phase.UPDATE, ctx -> log.add("b"));
    scheduler.register(Phase.UPDATE, ctx -> log.add("c"));
    scheduler.runFrame(0f);
    assertEquals(List.of("a", "b", "c"), log);
  }

  @Test
  void runsPhasesInFixedOrderRegardlessOfRegistrationOrder() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<String> log = new ArrayList<>();
    scheduler.register(Phase.LATE, ctx -> log.add("LATE"));
    scheduler.register(Phase.UPDATE, ctx -> log.add("UPDATE"));
    scheduler.register(Phase.FIXED, ctx -> log.add("FIXED"));
    scheduler.register(Phase.EARLY, ctx -> log.add("EARLY"));
    scheduler.runFrame(FIXED * 3f);
    assertEquals(List.of("EARLY", "FIXED", "FIXED", "FIXED", "UPDATE", "LATE"), log);
  }

  @Test
  void frameOfTwoAndAHalfFixedStepsRunsTwoFixedStepsAndKeepsRest() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<Phase> log = new ArrayList<>();
    scheduler.register(Phase.FIXED, ctx -> log.add(ctx.phase()));
    scheduler.runFrame(2.5f * FIXED);
    assertEquals(2, log.size());
    assertEquals(0.5 * FIXED, scheduler.accumulator(), 1e-6);
    scheduler.runFrame(2.5f * FIXED);
    assertEquals(5, log.size(), "Rest aus Frame 1 ergibt zusammen mit Frame 2 drei Schritte");
    assertEquals(0.0, scheduler.accumulator(), 1e-6);
  }

  @Test
  void toleratesFloatRoundingOfFrameDeltas() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<Phase> log = new ArrayList<>();
    scheduler.register(Phase.FIXED, ctx -> log.add(ctx.phase()));
    float frameDelta = 2.5f * FIXED; // 0.0499999970, knapp unter 2,5 Schritten
    for (int frame = 0; frame < 1000; frame++) {
      scheduler.runFrame(frameDelta);
    }
    assertEquals(2500, log.size());
  }

  @Test
  void runsNoFixedStepWhenFrameIsShorterThanFixedDelta() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<Phase> log = new ArrayList<>();
    scheduler.register(Phase.FIXED, ctx -> log.add(ctx.phase()));
    scheduler.runFrame(FIXED / 2f);
    assertEquals(0, log.size());
    scheduler.runFrame(FIXED / 2f);
    assertEquals(1, log.size());
  }

  @Test
  void providesTimeValues() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<Time> times = new ArrayList<>();
    scheduler.register(Phase.UPDATE, ctx -> times.add(ctx.time()));
    scheduler.register(Phase.FIXED, ctx -> times.add(ctx.time()));
    scheduler.runFrame(0.05f);
    scheduler.runFrame(0.01f);
    assertEquals(new Time(0.05f, FIXED, 0L), times.get(0));
    assertEquals(new Time(0.05f, FIXED, 0L), times.get(2));
    assertEquals(new Time(0.01f, FIXED, 1L), times.get(3));
    assertEquals(2L, scheduler.frameCount());
  }

  @Test
  void hooksRunAroundEveryPhaseStep() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<String> log = new ArrayList<>();
    scheduler.setHooks(
        new PhaseHooks() {
          @Override
          public void beforeCallbacks(FrameContext context) {
            log.add("before " + context.phase());
          }

          @Override
          public void afterStep(Phase phase) {
            log.add("after " + phase);
          }
        });
    scheduler.register(Phase.FIXED, ctx -> log.add("callback FIXED"));
    scheduler.runFrame(2f * FIXED);
    assertEquals(
        List.of(
            "before EARLY",
            "after EARLY",
            "before FIXED",
            "callback FIXED",
            "after FIXED",
            "before FIXED",
            "callback FIXED",
            "after FIXED",
            "before UPDATE",
            "after UPDATE",
            "before LATE",
            "after LATE"),
        log);
  }

  @Test
  void reportsCurrentPhaseOnlyDuringStep() {
    Scheduler scheduler = new Scheduler(FIXED);
    List<Phase> seen = new ArrayList<>();
    scheduler.register(Phase.UPDATE, ctx -> seen.add(scheduler.currentPhase()));
    scheduler.runFrame(0f);
    assertEquals(List.of(Phase.UPDATE), seen);
    assertNull(scheduler.currentPhase());
  }

  @Test
  void sameInputGivesSameCallSequenceOver1000Frames() {
    assertEquals(callSequence(), callSequence());
  }

  private static List<String> callSequence() {
    Scheduler scheduler = new Scheduler(1f / 60f);
    List<String> log = new ArrayList<>();
    for (Phase phase : Phase.values()) {
      scheduler.register(phase, ctx -> log.add(ctx.time().frame() + ":" + ctx.phase() + ":a"));
      scheduler.register(phase, ctx -> log.add(ctx.time().frame() + ":" + ctx.phase() + ":b"));
    }
    for (int frame = 0; frame < 1000; frame++) {
      float delta = (frame % 7) * 0.005f;
      scheduler.runFrame(delta);
    }
    log.add("rest=" + scheduler.accumulator());
    return log;
  }

  @Test
  void rejectsInvalidArguments() {
    assertThrows(IllegalArgumentException.class, () -> new Scheduler(0f));
    assertThrows(IllegalArgumentException.class, () -> new Scheduler(Float.NaN));
    Scheduler scheduler = new Scheduler(FIXED);
    assertThrows(IllegalArgumentException.class, () -> scheduler.runFrame(-1f));
  }

  @Test
  void rejectsNestedFrame() {
    Scheduler scheduler = new Scheduler(FIXED);
    scheduler.register(Phase.UPDATE, ctx -> scheduler.runFrame(0f));
    assertThrows(IllegalStateException.class, () -> scheduler.runFrame(0f));
  }
}
