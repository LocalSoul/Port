package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.IrHeader;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.sexpr.SBareInteger;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SExprReader;
import io.github.localsoul.port.ir.sexpr.SList;
import java.util.ArrayList;
import java.util.List;

/**
 * Liest IR-Text ({@code docs/ir-spec.md}) in den AST.
 *
 * <p>Verhalten:
 *
 * <ul>
 *   <li>Die Datei beginnt mit {@code (ir :version 1 :assembly "...")}, danach folgen
 *       Typdeklarationen.
 *   <li>Unbekannte Knotennamen werden abgelehnt, unbekannte Keywords ignoriert (Spec Abschnitt 11).
 *       Ein unbekanntes Keyword nimmt das folgende Element als Wert mit, wenn es kein Keyword ist.
 *   <li>Knoten des nativen Dialekts (Spec Abschnitt 12) werden mit „nativer Dialekt nicht
 *       unterstützt" abgelehnt (bis M10b).
 *   <li>Zahlen werden auf ihren Wertebereich geprüft. Zahlen ohne Suffix sind nur in {@code
 *       :version}, {@code (char ...)} und {@code :size} erlaubt (ADR-024).
 *   <li>Der Reader bricht beim ersten Fehler ab. Mitglieder bleiben in Eingabereihenfolge, die
 *       kanonische Sortierung ist Sache des Printers.
 * </ul>
 */
public final class IrReader {

  /** Höchste unterstützte IR-Version. */
  public static final int SUPPORTED_VERSION = 1;

  /** Erzeugt einen Reader. Er ist zustandslos und kann wiederverwendet werden. */
  public IrReader() {}

  /**
   * Liest eine IR-Datei.
   *
   * @param text IR-Text
   * @param fileName Dateiname für Positionsangaben
   * @return der AST
   * @throws IrSyntaxException bei Syntaxfehlern, unbekannten Knotennamen, falscher Stelligkeit,
   *     falschem Argumenttyp oder nicht unterstützter Version; die Position zeigt auf die
   *     Fehlerstelle
   */
  public IrFile read(String text, String fileName) throws IrSyntaxException {
    List<SExpr> topLevel = SExprReader.readAll(text, fileName);
    if (topLevel.isEmpty()) {
      throw new IrSyntaxException(
          new SourceSpan(fileName, 1, 1), "Dateikopf (ir :version ... :assembly ...) fehlt");
    }
    IrHeader header = readHeader(topLevel.get(0));
    List<TypeDecl> types = new ArrayList<>();
    for (int i = 1; i < topLevel.size(); i++) {
      types.add(DeclReader.readType(topLevel.get(i)));
    }
    return new IrFile(fileName, header, types);
  }

  private static IrHeader readHeader(SExpr expr) throws IrSyntaxException {
    SList list = Expect.list(expr, "Dateikopf (ir ...)");
    if (!Form.headOf(list).equals("ir")) {
      throw new IrSyntaxException(
          list.span(), "Dateikopf (ir ...) erwartet, gefunden '" + Form.headOf(list) + "'");
    }
    Form form = Form.of(list, KeywordSpec.values("version", "assembly", "sha256"));
    form.requireArgs(0, 0, "(ir :version nat :assembly string [:sha256 string])");
    SExpr versionExpr = form.requiredValue("version");
    if (!(versionExpr instanceof SBareInteger versionNumber)) {
      throw Expect.expected(versionExpr, "Zahl ohne Suffix (:version)");
    }
    int version = NumberLiterals.intValue(versionNumber);
    if (version < 1 || version > SUPPORTED_VERSION) {
      throw new IrSyntaxException(
          versionExpr.span(), "IR-Version " + version + " nicht unterstützt");
    }
    String assembly = Expect.string(form.requiredValue("assembly"), ":assembly");
    String sha256 = form.optionalString("sha256");
    return new IrHeader(version, assembly, sha256, form.nodeAttrs(), form.span());
  }
}
