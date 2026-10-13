package io.github.localsoul.port.ir.ast;

/** Typdeklaration (Spec Abschnitt 5). */
public sealed interface TypeDecl extends IrNode
    permits ClassDecl, StructDecl, InterfaceDecl, EnumDecl {

  /**
   * Liefert den qualifizierten Namen des Typs.
   *
   * @return den qualifizierten Namen des Typs
   */
  String name();
}
