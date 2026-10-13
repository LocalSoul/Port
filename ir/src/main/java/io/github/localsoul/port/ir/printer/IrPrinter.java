package io.github.localsoul.port.ir.printer;

import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.TypeDecl;
import java.util.List;

/**
 * Druckt einen AST in die kanonische Textform ({@code docs/ir-spec.md} Abschnitt 8).
 *
 * <ul>
 *   <li>Typen nach ID sortiert, Mitglieder nach Gruppe (Felder, Properties, Events, Konstruktoren,
 *       Methoden) und innerhalb der Gruppe nach ID; Enum-Werte nach Namen.
 *   <li>Keywords in der Reihenfolge der Grammatik, danach {@code :loc}, {@code :manual}, {@code
 *       :note}. Leere optionale Listen ({@code :mods ()}, {@code :attrs ()}, ...) entfallen.
 *   <li>Zeilenumbruch nach der Regel in {@link Layout}: höchstens 100 Zeichen für einzeilige
 *       Listen, 2 Leerzeichen Einrückung, nicht-leere Blöcke und Typen immer mehrzeilig.
 *   <li>Kopf, dann je eine Leerzeile vor jedem Typ; genau ein abschließendes {@code \n}.
 * </ul>
 *
 * <p>Die Ausgabe ist deterministisch: gleicher AST (ohne Positionen) ergibt byte-identischen Text.
 */
public final class IrPrinter {

  /** Erzeugt einen Printer. Er ist zustandslos und kann wiederverwendet werden. */
  public IrPrinter() {}

  /**
   * Druckt eine Datei.
   *
   * @param file AST
   * @return kanonischer IR-Text, endet mit genau einem {@code \n}
   * @throws IllegalArgumentException wenn ein Literal oder Bezeichner Attribute trägt (in der
   *     Textform nicht darstellbar)
   */
  public String print(IrFile file) {
    StringBuilder out = new StringBuilder();
    out.append(Layout.render(DeclDocs.header(file.header()), 0, 0)).append('\n');
    List<TypeDecl> types = CanonicalOrder.types(file.types());
    for (TypeDecl type : types) {
      out.append('\n');
      out.append(Layout.render(DeclDocs.type(type), 0, 0)).append('\n');
    }
    return out.toString();
  }
}
