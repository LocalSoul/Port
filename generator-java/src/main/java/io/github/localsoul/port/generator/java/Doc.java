package io.github.localsoul.port.generator.java;

import java.util.List;

/**
 * Layout-Modell für Java-Ausdrücke, damit {@link LineBreaker} wie google-java-format umbrechen
 * kann: unteilbarer Text, Aufruf mit Argumenten, Kette von Zugriffen.
 */
sealed interface Doc permits Doc.Text, Doc.Call, Doc.Chain {

  /** Unteilbarer Text. */
  record Text(String text) implements Doc {}

  /**
   * Aufruf {@code head(args)}, z. B. {@code Vector3.mul(a, b)}, {@code new Vector3(x)} oder als
   * Kettenglied {@code .translate(v)}.
   */
  record Call(String head, List<Doc> args) implements Doc {

    /** Kopiert die Argumente. */
    public Call {
      args = List.copyOf(args);
    }
  }

  /** Kette {@code first.seg1.seg2...}; Glieder sind {@link Call} mit {@code "."}-Kopf oder Text. */
  record Chain(Doc first, List<Doc> segments) implements Doc {

    /** Kopiert die Glieder. */
    public Chain {
      segments = List.copyOf(segments);
    }
  }

  /** Einzeilige Form. */
  static String flat(Doc doc) {
    return switch (doc) {
      case Text t -> t.text();
      case Call c -> c.head() + "(" + joinFlat(c.args()) + ")";
      case Chain c -> {
        StringBuilder out = new StringBuilder(flat(c.first()));
        for (Doc segment : c.segments()) {
          out.append(flat(segment));
        }
        yield out.toString();
      }
    };
  }

  /** Argumente einzeilig, durch {@code ", "} getrennt. */
  static String joinFlat(List<Doc> docs) {
    StringBuilder out = new StringBuilder();
    for (int i = 0; i < docs.size(); i++) {
      out.append(i == 0 ? "" : ", ").append(flat(docs.get(i)));
    }
    return out.toString();
  }
}
