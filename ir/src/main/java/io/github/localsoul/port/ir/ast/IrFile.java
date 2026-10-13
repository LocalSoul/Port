package io.github.localsoul.port.ir.ast;

import java.util.List;

/**
 * Eine IR-Datei: Kopf und Typdeklarationen in Eingabereihenfolge.
 *
 * @param fileName Dateiname, wie er dem Reader übergeben wurde
 * @param header Dateikopf
 * @param types Typdeklarationen
 */
public record IrFile(String fileName, IrHeader header, List<TypeDecl> types) {

  /** Kopiert die Typen in eine unveränderliche Liste. */
  public IrFile {
    types = List.copyOf(types);
  }
}
