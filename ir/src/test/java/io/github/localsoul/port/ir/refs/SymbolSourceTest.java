package io.github.localsoul.port.ir.refs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SymbolSourceTest {

  @Test
  void cutsBalancedExpressionIgnoringStringsAndComments() {
    String text = "(a\n  (b \"x)\" ; c)\n   (d))\n  (e))\n";
    assertEquals("(b \"x)\" ; c)\n   (d))", SymbolSource.slice(text, 2, 3));
    assertEquals("(e)", SymbolSource.slice(text, 4, 3));
    assertEquals("a", SymbolSource.slice(text, 1, 2));
    assertEquals("", SymbolSource.slice(text, 99, 1));
    assertEquals(SymbolSource.sha256("(e)"), SymbolSource.hash(text, 4, 3));
    assertEquals(64, SymbolSource.sha256("x").length());
  }
}
