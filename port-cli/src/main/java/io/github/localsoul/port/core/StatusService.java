package io.github.localsoul.port.core;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SExprReader;
import io.github.localsoul.port.ir.sexpr.SKeyword;
import io.github.localsoul.port.ir.sexpr.SList;
import io.github.localsoul.port.ir.sexpr.SSymbol;
import io.github.localsoul.port.json.JsonObject;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * {@code port status}: {@code phase} (zuletzt abgeschlossene Phase), {@code classes} (Typen in
 * {@code ir/}), {@code manualOpen} (Handarbeitsstellen ohne {@code manual/}-Datei), {@code
 * irChanged} (braucht den Index, in M4 immer 0 mit Hinweis).
 */
public final class StatusService {

  private StatusService() {}

  /**
   * Ermittelt den Stand des Projekts.
   *
   * @param context globale Einstellungen
   * @return Ergebnis; offene Handarbeit als {@code PORT-MANUAL}-Warnung je Typ
   * @throws PortException Exit 2 ohne gültiges Projekt oder bei ungültiger IR
   */
  public static CommandResult status(CommandContext context) throws PortException {
    Project project = Project.open(context);
    List<IrFile> files = IrFiles.read(project.dir());
    Set<String> covered = GenStep.manualTypes(project.dir(), files);
    List<Diagnostic> diagnostics = new ArrayList<>();
    int classes = 0;
    int manualOpen = 0;
    for (IrFile file : files) {
      classes += file.types().size();
      int open = countManual(project, file);
      if (open > 0 && !coversAll(file, covered)) {
        manualOpen += open;
        diagnostics.add(
            Diagnostic.warn(
                DiagnosticIds.MANUAL,
                open + " Handarbeitsstelle(n) (raw/:manual) ohne manual/-Klasse",
                file.fileName()));
      }
    }
    diagnostics.add(
        Diagnostic.info(DiagnosticIds.LATER, "irChanged braucht den Index (M5), hier immer 0"));
    JsonObject data =
        JsonObject.builder()
            .put("phase", ProjectState.phase(project.dir()))
            .put("classes", classes)
            .put("manualOpen", manualOpen)
            .put("irChanged", 0)
            .build();
    return CommandResult.ok("status", data, diagnostics);
  }

  private static boolean coversAll(IrFile file, Set<String> covered) {
    for (var type : file.types()) {
      if (!covered.contains(type.name())) {
        return false;
      }
    }
    return true;
  }

  /** Zählt die Handarbeitsstellen einer IR-Datei. */
  private static int countManual(Project project, IrFile file) throws PortException {
    List<SExpr> roots;
    try {
      String text = Files.readString(project.resolve(file.fileName()), StandardCharsets.UTF_8);
      roots = SExprReader.readAll(text, file.fileName());
    } catch (IrSyntaxException e) {
      throw new PortException(
          ExitCode.INVALID,
          Diagnostic.error(DiagnosticIds.IR, e.getMessage(), e.span().toString(), null));
    } catch (IOException e) {
      throw new PortException(
          ExitCode.GENERAL,
          Diagnostic.error(DiagnosticIds.IO, e.getMessage(), file.fileName(), null));
    }
    int count = 0;
    for (SExpr root : roots) {
      count += countManual(root);
    }
    return count;
  }

  /**
   * Eine Stelle je Knoten mit {@code :manual} (samt Teilbaum, das Frontend setzt es an das Member
   * mit dem {@code raw}) bzw. je {@code (raw ...)}/{@code (manual ...)} ohne solches Member.
   */
  private static int countManual(SExpr expr) {
    if (!(expr instanceof SList list) || list.items().isEmpty()) {
      return 0;
    }
    if (list.items().get(0) instanceof SSymbol head
        && (head.text().equals("raw") || head.text().equals("manual"))) {
      return 1;
    }
    for (SExpr item : list.items()) {
      if (item instanceof SKeyword keyword && keyword.name().equals("manual")) {
        return 1;
      }
    }
    int count = 0;
    for (SExpr item : list.items()) {
      count += countManual(item);
    }
    return count;
  }
}
