package io.github.localsoul.port.engine.core.subsystems.render;

import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.input.KeyEvent;
import io.github.localsoul.port.engine.api.render.Affine2D;
import io.github.localsoul.port.engine.api.render.RenderBackend;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** Backend für Tests: protokolliert Befehle als Text, Tasten werden von außen eingespeist. */
public final class RecordingBackend implements RenderBackend {

  public final List<String> log = new ArrayList<>();
  public final List<KeyEvent> keys = new ArrayList<>();
  private final int width;
  private final int height;
  public boolean closed;

  public RecordingBackend(int width, int height) {
    this.width = width;
    this.height = height;
  }

  @Override
  public int width() {
    return width;
  }

  @Override
  public int height() {
    return height;
  }

  @Override
  public void begin(int background) {
    log.add("begin " + Integer.toHexString(background));
  }

  @Override
  public void drawTexture(Texture texture, Affine2D t, int tint) {
    log.add(
        String.format(
            Locale.ROOT,
            "texture %dx%d [%.2f %.2f %.2f | %.2f %.2f %.2f]",
            texture.width(),
            texture.height(),
            t.m00() + 0.0,
            t.m01() + 0.0,
            t.m02() + 0.0,
            t.m10() + 0.0,
            t.m11() + 0.0,
            t.m12() + 0.0));
  }

  @Override
  public void fillRect(float x, float y, float w, float h, int fill, int border) {
    log.add(String.format(Locale.ROOT, "rect %.0f %.0f %.0f %.0f", x, y, w, h));
  }

  @Override
  public void drawText(
      String text,
      float x,
      float y,
      float w,
      float h,
      int size,
      boolean bold,
      int anchor,
      int color) {
    log.add("text " + text + " size=" + size + " bold=" + bold + " anchor=" + anchor);
  }

  @Override
  public void present() {
    log.add("present");
  }

  @Override
  public List<KeyEvent> pollKeys() {
    List<KeyEvent> result = List.copyOf(keys);
    keys.clear();
    return result;
  }

  @Override
  public boolean closed() {
    return closed;
  }

  @Override
  public void close() {
    log.add("close");
  }
}
