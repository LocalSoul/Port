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
    String exact = status.get(symbol);
    if (exact != null) {
      return exact;
    }
    return status.get(genericKey(symbol));
  }

  /**
   * Methoden-Typargumente durch Platzhalter ersetzt ({@code AddComponent<UnityEngine.X>#} → {@code
   * AddComponent<T>#}, mehrere: {@code <T1,T2>}), wie der Generator das Mapping sucht.
   */
  static String genericKey(String symbol) {
    int member = symbol.indexOf("::");
    if (member < 0) {
      return symbol;
    }
    int open = symbol.indexOf('<', member + 2);
    int hash = symbol.indexOf('#', member + 2);
    if (open < 0 || (hash >= 0 && open > hash)) {
      return symbol;
    }
    int depth = 0;
    int count = 1;
    int close = open;
    for (int i = open; i < symbol.length(); i++) {
      char c = symbol.charAt(i);
      if (c == '<') {
        depth++;
      } else if (c == '>') {
        depth--;
        if (depth == 0) {
          close = i;
          break;
        }
      } else if (c == ',' && depth == 1) {
        count++;
      }
    }
    StringBuilder placeholders = new StringBuilder();
    for (int i = 1; i <= count; i++) {
      placeholders.append(i == 1 ? "" : ",").append(count == 1 ? "T" : "T" + i);
    }
    return symbol.substring(0, open + 1) + placeholders + symbol.substring(close);
  }
}
