package io.github.localsoul.port.ir.validate;

/**
 * Kennt Typen außerhalb der geprüften Datei (andere IR-Dateien, Adapter-API, {@code System.*}).
 *
 * <p>Ohne Katalog meldet der Validator externe Typen als {@code info} (IR002, nicht geprüft). Mit
 * Katalog ist ein dort unbekannter externer Typ ein Fehler.
 */
public interface TypeCatalog {

  /**
   * Prüft, ob ein Typ bekannt ist.
   *
   * @param qualifiedName qualifizierter Name ohne Typargumente, z. B. {@code UnityEngine.Vector3}
   * @return {@code true}, wenn der Typ existiert
   */
  boolean containsType(String qualifiedName);
}
