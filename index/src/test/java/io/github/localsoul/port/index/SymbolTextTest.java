package io.github.localsoul.port.index;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SymbolTextTest {

  @Test
  void cutsBalancedExpressionIgnoringStringsAndComments() {
    String text = "(a\n  (b \"x)\" ; c)\n   (d))\n  (e))\n";
    assertEquals("(b \"x)\" ; c)\n   (d))", SymbolText.slice(text, 2, 3));
    assertEquals("(e)", SymbolText.slice(text, 4, 3));
    assertEquals("a", SymbolText.slice(text, 1, 2));
    assertEquals("", SymbolText.slice(text, 99, 1));
  }
}
