package io.github.localsoul.port.lint;

import java.util.Map;
import java.util.TreeMap;

/**
 * Was Adapter und {@code runtime-system} bereitstellen: Symbol-ID oder Typname → {@code full} oder
 * {@code stub} (Nutzerentscheidung M6: Einträge in {@code mapping.tsv} zählen als {@code full},
 * {@code adapter-metadata.json} kennzeichnet Stubs).
 */
public final class Coverage {

  /** Vollständig umgesetzt. */
  public static final String FULL = "full";

  /** Nur als Stub vorhanden. */
  public static final String STUB = "stub";

  private final Map<String, String> status = new TreeMap<>();

  /**
   * Trägt ein Symbol ein; Typzeilen mit Stelligkeit ({@code System.Func/2}) zählen für den Typ. Ein
   * Stub bleibt Stub, auch wenn das Symbol zusätzlich im Mapping steht.
   *
   * @param symbol Symbol-ID oder Typname
   * @param value {@link #FULL} oder {@link #STUB}
   */
  public void put(String symbol, String value) {
    int arity = symbol.indexOf('/');
    String key = arity < 0 ? symbol : symbol.substring(0, arity);
    if (STUB.equals(status.get(key))) {
      return;
    }
    status.put(key, value);
  }

  /**
   * Stand eines Symbols.
   *
   * @param symbol Symbol-ID oder Typname
   * @return {@link #FULL}, {@link #STUB} oder {@code null}, wenn es fehlt
   */
  public String status(String symbol) {
    return status.get(symbol);
  }
}
