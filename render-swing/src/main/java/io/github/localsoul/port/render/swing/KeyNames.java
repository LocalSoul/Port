package io.github.localsoul.port.render.swing;

import java.awt.event.KeyEvent;

/**
 * AWT-Tastencodes → neutrale Tastennamen ({@link
 * io.github.localsoul.port.engine.api.input.KeyEvent}).
 */
final class KeyNames {

  private KeyNames() {}

  /**
   * Name einer Taste.
   *
   * @param code {@link KeyEvent#getKeyCode()}
   * @return neutraler Name oder {@code null} für nicht unterstützte Tasten
   */
  static String of(int code) {
    if (code >= KeyEvent.VK_A && code <= KeyEvent.VK_Z) {
      return String.valueOf((char) ('A' + (code - KeyEvent.VK_A)));
    }
    if (code >= KeyEvent.VK_0 && code <= KeyEvent.VK_9) {
      return String.valueOf((char) ('0' + (code - KeyEvent.VK_0)));
    }
    return switch (code) {
      case KeyEvent.VK_LEFT -> "LEFT";
      case KeyEvent.VK_RIGHT -> "RIGHT";
      case KeyEvent.VK_UP -> "UP";
      case KeyEvent.VK_DOWN -> "DOWN";
      case KeyEvent.VK_SPACE -> "SPACE";
      case KeyEvent.VK_ENTER -> "ENTER";
      case KeyEvent.VK_ESCAPE -> "ESCAPE";
      case KeyEvent.VK_TAB -> "TAB";
      case KeyEvent.VK_BACK_SPACE -> "BACKSPACE";
      case KeyEvent.VK_SHIFT -> "SHIFT";
      case KeyEvent.VK_CONTROL -> "CONTROL";
      case KeyEvent.VK_ALT -> "ALT";
      default -> null;
    };
  }
}
