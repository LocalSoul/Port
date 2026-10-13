package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.EnumDecl;
import io.github.localsoul.port.ir.ast.EnumMember;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.MemberDecl;
import io.github.localsoul.port.ir.ast.SymbolIds;
import io.github.localsoul.port.ir.ast.TypeDecl;
import java.util.HashMap;
import java.util.Map;

/**
 * IR004: doppelte Symbol-ID. Gemeldet wird jedes weitere Vorkommen, mit Verweis auf das erste.
 * Geprüft werden Typnamen der Datei und Mitglieds-IDs je Typ (Felder, Properties und Events teilen
 * sich {@code Typ::name}).
 */
final class DuplicateSymbolRule {

  private DuplicateSymbolRule() {}

  static void check(IrFile file, DiagnosticSink sink) {
    // Nur für Lookups, nie iteriert.
    Map<String, SourceSpan> typeIds = new HashMap<>();
    for (TypeDecl type : file.types()) {
      String id = SymbolIds.of(type);
      register(typeIds, id, type.span(), sink);
      checkMembers(type, sink);
    }
  }

  private static void checkMembers(TypeDecl type, DiagnosticSink sink) {
    Map<String, SourceSpan> memberIds = new HashMap<>();
    if (type instanceof EnumDecl enumDecl) {
      for (EnumMember member : enumDecl.members()) {
        register(memberIds, SymbolIds.of(type.name(), member), member.span(), sink);
      }
      return;
    }
    for (MemberDecl member : TypeMembers.of(type)) {
      register(memberIds, SymbolIds.of(type.name(), member), member.span(), sink);
    }
  }

  private static void register(
      Map<String, SourceSpan> seen, String id, SourceSpan span, DiagnosticSink sink) {
    SourceSpan first = seen.get(id);
    if (first == null) {
      seen.put(id, span);
      return;
    }
    sink.add(
        "IR004",
        Severity.ERROR,
        id,
        "Symbol '" + id + "' doppelt, zuerst bei " + first,
        span,
        "eine Deklaration umbenennen oder entfernen");
  }
}
