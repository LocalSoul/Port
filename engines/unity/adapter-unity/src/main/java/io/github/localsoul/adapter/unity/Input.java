package io.github.localsoul.adapter.unity;

import io.github.localsoul.port.engine.api.schedule.FrameContext;

/** {@code UnityEngine.Input}. */
public final class Input {

  private Input() {}

  /**
   * {@code Input.GetAxis}, Status {@code stub}: liefert den Rohwert der Achse ohne Unitys Glättung
   * (Sensitivity, Gravity). Die echte Glättung folgt später im Adapter (ADR-015).
   *
   * @param axisName Name der Achse
   * @return Rohwert; außerhalb eines Frames 0
   */
  public static float getAxis(String axisName) {
    FrameContext context = UnityRuntime.current();
    return context == null ? 0f : context.input().axis(axisName);
  }
}
