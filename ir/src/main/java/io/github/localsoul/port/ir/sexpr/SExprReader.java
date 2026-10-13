package io.github.localsoul.port.ir.sexpr;

import io.github.localsoul.port.ir.IrSyntaxException;
import java.util.ArrayList;
import java.util.List;

/**
 * Baut aus IR-Text eine Folge von {@link SExpr}-Knoten mit Quellpositionen.
 *
 * <p>Der Reader prüft nur Lexik und Klammerung. Welche Knotennamen und Keywords erlaubt sind,
 * entscheidet die AST-Ebene.
 */
public final class SExprReader {

  private SExprReader() {}

  /**
   * Liest alle Ausdrücke der obersten Ebene.
   *
   * @param text IR-Text
   * @param fileName Dateiname für Positionsangaben
   * @return Ausdrücke in Eingabereihenfolge, leer bei leerer Eingabe
   * @throws IrSyntaxException bei lexikalischen Fehlern, nicht geschlossener Klammer (Position der
   *     öffnenden Klammer) oder überzähliger schließender Klammer
   */
  public static List<SExpr> readAll(String text, String fileName) throws IrSyntaxException {
    IrLexer lexer = new IrLexer(text, fileName);
    List<SExpr> result = new ArrayList<>();
    Token token = lexer.next();
    while (token.kind() != TokenKind.EOF) {
      SExpr expr = readExpr(lexer, token);
      result.add(expr);
      token = lexer.next();
    }
    return List.copyOf(result);
  }

  private static SExpr readExpr(IrLexer lexer, Token token) throws IrSyntaxException {
    switch (token.kind()) {
      case LPAREN:
        return readList(lexer, token);
      case RPAREN:
        throw new IrSyntaxException(token.span(), "unerwartete schließende Klammer ')'");
      case STRING:
        return new SString(token.value(), token.span());
      case NUMBER:
        return new SNumber(token.text(), token.suffix(), token.span());
      case BARE_INTEGER:
        return new SBareInteger(token.text(), token.span());
      case KEYWORD:
        return new SKeyword(token.value(), token.span());
      case SYMBOL:
        return new SSymbol(token.text(), token.span());
      case EOF:
      default:
        throw new IllegalStateException("EOF wird vom Aufrufer behandelt: " + token);
    }
  }

  /**
   * Liest eine Liste bis zur passenden schließenden Klammer. Rekursiv, die Tiefe ist durch die
   * Schachtelung der Eingabe begrenzt.
   */
  private static SList readList(IrLexer lexer, Token open) throws IrSyntaxException {
    List<SExpr> items = new ArrayList<>();
    Token token = lexer.next();
    while (token.kind() != TokenKind.RPAREN) {
      if (token.kind() == TokenKind.EOF) {
        throw new IrSyntaxException(open.span(), "Klammer '(' nicht geschlossen");
      }
      SExpr item = readExpr(lexer, token);
      items.add(item);
      token = lexer.next();
    }
    return new SList(items, open.span());
  }
}
