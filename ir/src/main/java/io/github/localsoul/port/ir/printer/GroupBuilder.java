package io.github.localsoul.port.ir.printer;

import io.github.localsoul.port.ir.ast.NodeAttrs;
import java.util.ArrayList;
import java.util.List;

/** Baut eine {@link Doc.Group} Element für Element. */
final class GroupBuilder {

  private final String head;
  private final List<Doc.Item> items = new ArrayList<>();
  private int headCount = Integer.MAX_VALUE;
  private boolean forceBreak;

  private GroupBuilder(String head) {
    this.head = head;
  }

  /** Liste mit Knotenname. */
  static GroupBuilder named(String head) {
    return new GroupBuilder(head);
  }

  /** Liste ohne Kopf, z. B. Parameterliste oder Modifier. */
  static GroupBuilder headless() {
    return new GroupBuilder("");
  }

  GroupBuilder atom(String text) {
    items.add(new Doc.Item(null, new Doc.Atom(text)));
    return this;
  }

  GroupBuilder doc(Doc doc) {
    items.add(new Doc.Item(null, doc));
    return this;
  }

  GroupBuilder docs(List<Doc> docs) {
    for (Doc doc : docs) {
      doc(doc);
    }
    return this;
  }

  /** {@code :keyword value}, nichts bei {@code value == null}. */
  GroupBuilder keyword(String keyword, Doc value) {
    if (value != null) {
      items.add(new Doc.Item(keyword, value));
    }
    return this;
  }

  /** {@code :keyword atom}, nichts bei {@code text == null}. */
  GroupBuilder keywordAtom(String keyword, String text) {
    if (text != null) {
      items.add(new Doc.Item(keyword, new Doc.Atom(text)));
    }
    return this;
  }

  /** {@code :keyword "text"}, nichts bei {@code text == null}. */
  GroupBuilder keywordString(String keyword, String text) {
    if (text != null) {
      keywordAtom(keyword, StringEscapes.quote(text));
    }
    return this;
  }

  /** Flag ohne Wert, z. B. {@code :abstract}. */
  GroupBuilder flag(String keyword) {
    items.add(new Doc.Item(null, new Doc.Atom(":" + keyword)));
    return this;
  }

  /** Alle ab jetzt hinzugefügten Elemente stehen beim Umbruch auf eigenen Zeilen. */
  GroupBuilder bodyStartsHere() {
    headCount = items.size();
    return this;
  }

  GroupBuilder forceBreak(boolean force) {
    forceBreak = force;
    return this;
  }

  /** {@code :loc}, {@code :manual}, {@code :note} in dieser Reihenfolge (Spec Abschnitt 8). */
  GroupBuilder attrs(NodeAttrs attrs) {
    keywordString("loc", attrs.loc());
    keywordString("manual", attrs.manual());
    keywordString("note", attrs.note());
    return this;
  }

  Doc.Group build() {
    return new Doc.Group(head, items, headCount, forceBreak);
  }
}
