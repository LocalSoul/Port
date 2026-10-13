package io.github.localsoul.port.ir.printer;

import java.util.List;

/** Layout-Modell: ein Atom oder eine Liste mit Kopf und Elementen. */
sealed interface Doc permits Doc.Atom, Doc.Group {

  /** Fertig gedrucktes Atom, z. B. {@code i32} oder {@code "text"}. */
  record Atom(String text) implements Doc {}

  /**
   * Liste {@code (head item...)}.
   *
   * @param head Knotenname, leer bei Listen ohne Kopf wie {@code (public static)}
   * @param items Elemente in Druckreihenfolge
   * @param headCount wie viele Elemente höchstens in die erste Zeile dürfen, wenn die Liste
   *     umbricht
   * @param forceBreak {@code true}, wenn die Liste nie einzeilig gedruckt wird
   */
  record Group(String head, List<Item> items, int headCount, boolean forceBreak) implements Doc {

    public Group {
      items = List.copyOf(items);
    }
  }

  /**
   * Element einer Liste, optional mit Keyword davor ({@code :init expr}).
   *
   * @param keyword Keyword ohne Doppelpunkt, oder {@code null}
   * @param value Wert
   */
  record Item(String keyword, Doc value) {}
}
