package io.github.localsoul.port.render.swing;

import io.github.localsoul.port.engine.api.component.Texture;
import io.github.localsoul.port.engine.api.input.KeyEvent;
import io.github.localsoul.port.engine.api.render.Affine2D;
import io.github.localsoul.port.engine.api.render.RenderBackend;
import java.awt.Canvas;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.KeyAdapter;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;

/**
 * Backend mit Fenster (ADR-014): Swing-Fenster mit fester Größe, Java2D wie {@link
 * OffscreenBackend}, das fertige Bild wird über eine BufferStrategy angezeigt. Tasten kommen über
 * AWT und werden als {@link KeyEvent} gesammelt; Schließen des Fensters setzt {@link #closed()}.
 */
public final class WindowBackend implements RenderBackend {

  private final Java2DCanvas canvas;
  private final ConcurrentLinkedQueue<KeyEvent> keys = new ConcurrentLinkedQueue<>();
  private volatile boolean closed;
  private JFrame frame;
  private Canvas surface;
  private BufferStrategy strategy;

  /**
   * Öffnet das Fenster.
   *
   * @param title Fenstertitel
   * @param width Breite der Zeichenfläche
   * @param height Höhe der Zeichenfläche
   * @throws IllegalStateException wenn das Fenster nicht geöffnet werden kann (z. B. headless)
   */
  public WindowBackend(String title, int width, int height) {
    this.canvas = new Java2DCanvas(width, height);
    try {
      SwingUtilities.invokeAndWait(() -> open(title, width, height));
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IllegalStateException("Fenster nicht geöffnet: unterbrochen", e);
    } catch (InvocationTargetException e) {
      throw new IllegalStateException(
          "Fenster nicht geöffnet: " + e.getCause().getMessage(), e.getCause());
    }
  }

  private void open(String title, int width, int height) {
    frame = new JFrame(title);
    frame.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
    frame.setResizable(false);
    surface = new Canvas();
    surface.setPreferredSize(new Dimension(width, height));
    surface.setFocusable(true);
    surface.setIgnoreRepaint(true);
    KeyAdapter listener =
        new KeyAdapter() {
          @Override
          public void keyPressed(java.awt.event.KeyEvent event) {
            queue(event, true);
          }

          @Override
          public void keyReleased(java.awt.event.KeyEvent event) {
            queue(event, false);
          }
        };
    surface.addKeyListener(listener);
    frame.addWindowListener(
        new WindowAdapter() {
          @Override
          public void windowClosing(WindowEvent event) {
            closed = true;
          }

          @Override
          public void windowClosed(WindowEvent event) {
            closed = true;
          }
        });
    frame.add(surface);
    frame.pack();
    frame.setLocationRelativeTo(null);
    frame.setVisible(true);
    surface.createBufferStrategy(2);
    strategy = surface.getBufferStrategy();
    surface.requestFocusInWindow();
  }

  private void queue(java.awt.event.KeyEvent event, boolean down) {
    String name = KeyNames.of(event.getKeyCode());
    if (name != null) {
      keys.add(new KeyEvent(name, down));
    }
  }

  @Override
  public int width() {
    return canvas.image().getWidth();
  }

  @Override
  public int height() {
    return canvas.image().getHeight();
  }

  @Override
  public void begin(int background) {
    canvas.begin(background);
  }

  @Override
  public void drawTexture(Texture texture, Affine2D transform, int tint) {
    canvas.drawTexture(texture, transform, tint);
  }

  @Override
  public void fillRect(float x, float y, float width, float height, int fill, int border) {
    canvas.fillRect(x, y, width, height, fill, border);
  }

  @Override
  public void drawText(
      String text,
      float x,
      float y,
      float width,
      float height,
      int size,
      boolean bold,
      int anchor,
      int color) {
    canvas.drawText(text, x, y, width, height, size, bold, anchor, color);
  }

  @Override
  public void present() {
    canvas.finish();
    if (closed || strategy == null) {
      return;
    }
    do {
      do {
        Graphics graphics = strategy.getDrawGraphics();
        graphics.drawImage(canvas.image(), 0, 0, null);
        graphics.dispose();
      } while (strategy.contentsRestored());
      strategy.show();
    } while (strategy.contentsLost());
    java.awt.Toolkit.getDefaultToolkit().sync();
  }

  @Override
  public List<KeyEvent> pollKeys() {
    List<KeyEvent> result = new ArrayList<>();
    KeyEvent key = keys.poll();
    while (key != null) {
      result.add(key);
      key = keys.poll();
    }
    return result;
  }

  @Override
  public boolean closed() {
    return closed;
  }

  @Override
  public void close() {
    closed = true;
    canvas.finish();
    if (frame != null) {
      SwingUtilities.invokeLater(frame::dispose);
    }
  }
}
