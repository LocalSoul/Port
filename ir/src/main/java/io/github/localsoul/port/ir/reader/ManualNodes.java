package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.ManualNode;
import io.github.localsoul.port.ir.ast.RawLanguage;
import io.github.localsoul.port.ir.ast.RawNode;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SList;
import io.github.localsoul.port.ir.sexpr.SSymbol;

/** Liest {@code (raw cs|asm string)} und {@code (manual string)} (Spec Abschnitt 9). */
final class ManualNodes {

  private ManualNodes() {}

  static RawNode readRaw(SList list) throws IrSyntaxException {
    Form form = Form.of(list, KeywordSpec.NONE);
    form.requireArgs(2, 2, "(raw cs|asm string)");
    SExpr languageExpr = form.arg(0);
    RawLanguage language = null;
    if (languageExpr instanceof SSymbol symbol) {
      language = RawLanguage.fromSpelling(symbol.text());
    }
    if (language == null) {
      throw Expect.expected(languageExpr, "Sprache 'cs' oder 'asm'");
    }
    String text = Expect.string(form.arg(1), "Originaltext");
    return new RawNode(language, text, form.nodeAttrs(), form.span());
  }

  static ManualNode readManual(SList list) throws IrSyntaxException {
    Form form = Form.of(list, KeywordSpec.NONE);
    form.requireArgs(1, 1, "(manual string)");
    String reason = Expect.string(form.arg(0), "Grund");
    return new ManualNode(reason, form.nodeAttrs(), form.span());
  }
}
