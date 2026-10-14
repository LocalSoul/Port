package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.refs.Reference;
import io.github.localsoul.port.ir.refs.References;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Prüft eine gelesene IR-Datei und liefert Diagnosen ({@code docs/cli-contract.md}).
 *
 * <ul>
 *   <li><b>IR001</b> Syntaxfehler: entsteht im Reader, siehe {@link
 *       #syntaxError(IrSyntaxException)}.
 *   <li><b>IR002</b> hängende Referenz: Typ oder Member fehlt. Externe Typen werden über den {@link
 *       TypeCatalog} geprüft; ohne Katalog einmal pro Typname als {@code info} gemeldet.
 *   <li><b>IR003</b> Typ {@code unknown}.
 *   <li><b>IR004</b> doppelte Symbol-ID.
 *   <li><b>IR005</b> Vererbungszyklus.
 * </ul>
 *
 * <p>Der Validator bricht nie beim ersten Fund ab. Die Liste ist sortiert nach {@link
 * Diagnostic#ORDER} (Datei, Zeile, Spalte, ID). Lokale Variablen und Sprungmarken werden nicht
 * geprüft.
 */
public final class IrValidator {

  private final TypeCatalog catalog;

  /** Validator ohne Typkatalog: externe Typen werden als {@code info} gemeldet. */
  public IrValidator() {
    this.catalog = null;
  }

  /**
   * Validator mit Typkatalog: externe Typen, die der Katalog nicht kennt, sind Fehler.
   *
   * @param catalog Katalog der externen Typen
   * @throws IllegalArgumentException wenn {@code catalog} {@code null} ist
   */
  public IrValidator(TypeCatalog catalog) {
    if (catalog == null) {
      throw new IllegalArgumentException("catalog darf nicht null sein, sonst new IrValidator()");
    }
    this.catalog = catalog;
  }

  /**
   * Prüft eine Datei.
   *
   * @param file gelesene Datei
   * @return alle Diagnosen, sortiert, leer bei gültiger Datei (unveränderlich)
   */
  public List<Diagnostic> validate(IrFile file) {
    DiagnosticSink sink = new DiagnosticSink();
    Map<String, TypeDecl> declaredTypes = declaredTypes(file);
    DuplicateSymbolRule.check(file, sink);
    InheritanceCycleRule.check(file, declaredTypes, sink);
    List<Reference> references = References.collect(file);
    ReferenceRule referenceRule = new ReferenceRule(declaredTypes, catalog, sink);
    referenceRule.check(references);
    return sink.sorted();
  }

  /**
   * Wandelt einen Syntaxfehler des Readers in eine Diagnose IR001.
   *
   * @param error Fehler des Readers
   * @return Diagnose ohne Symbol
   */
  public static Diagnostic syntaxError(IrSyntaxException error) {
    return new Diagnostic("IR001", Severity.ERROR, null, error.reason(), error.span(), null);
  }

  /** Erste Deklaration je Name; Duplikate meldet IR004. Nur für Lookups, nie iteriert. */
  private static Map<String, TypeDecl> declaredTypes(IrFile file) {
    Map<String, TypeDecl> result = new HashMap<>();
    for (TypeDecl type : file.types()) {
      result.putIfAbsent(type.name(), type);
    }
    return result;
  }
}
