package io.github.localsoul.adapter.unity;

import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;

/** {@code UnityEngine.Time}. */
public final class Time {

  private Time() {}

  /**
   * {@code Time.deltaTime}: Dauer des Frames; in {@code FixedUpdate} der feste Zeitschritt (wie
   * Unity). Außerhalb eines Frames 0.
   *
   * @return Sekunden
   */
  public static float deltaTime() {
    FrameContext context = UnityRuntime.current();
    if (context == null) {
      return 0f;
    }
    if (context.phase() == Phase.FIXED) {
      return context.time().fixedDelta();
    }
    return context.time().delta();
  }

  /**
   * {@code Time.fixedDeltaTime}.
   *
   * @return Sekunden; außerhalb eines Frames 0
   */
  public static float fixedDeltaTime() {
    FrameContext context = UnityRuntime.current();
    return context == null ? 0f : context.time().fixedDelta();
  }
}
