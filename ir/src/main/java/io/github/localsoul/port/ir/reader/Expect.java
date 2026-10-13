package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.Identifiers;
import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.Modifier;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.TypeRef;
import io.github.localsoul.port.ir.sexpr.SBareInteger;
import io.github.localsoul.port.ir.sexpr.SExpr;
import io.github.localsoul.port.ir.sexpr.SKeyword;
import io.github.localsoul.port.ir.sexpr.SList;
import io.github.localsoul.port.ir.sexpr.SNumber;
import io.github.localsoul.port.ir.sexpr.SString;
import io.github.localsoul.port.ir.sexpr.SSymbol;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/** Prüft einzelne Elemente und baut einheitliche Fehlermeldungen. */
final class Expect {

  /** Knotennamen des nativen Dialekts (Spec Abschnitt 12), bis M10b nicht unterstützt. */
  static final Set<String> DIALECT_NAMES =
      Set.of(
          "cfg",
          "bb",
          "store",
          "load",
          "ncall",
          "ncall-ind",
          "bitcast",
          "zext",
          "sext",
          "trunc",
          "rol",
          "ror",
          "addr-of",
          "intrin",
          "import",
          "br",
          "br-if",
          "br-switch",
          "br-ind",
          "ret",
          "unreachable",
          "bytes");

  private Expect() {}

  /** Fehler für Knoten oder Keywords des nativen Dialekts. */
  static IrSyntaxException dialect(SourceSpan span, String what) {
    return new IrSyntaxException(span, "nativer Dialekt nicht unterstützt: '" + what + "'");
  }

  /** Bezeichner nach {@code ident}. */
  static String ident(SExpr expr, String what) throws IrSyntaxException {
    if (expr instanceof SSymbol symbol && Identifiers.isIdent(symbol.text())) {
      return symbol.text();
    }
    throw expected(expr, "Bezeichner (" + what + ")");
  }

  /** Qualifizierter Name nach {@code qname}. */
  static String qualifiedName(SExpr expr, String what) throws IrSyntaxException {
    if (expr instanceof SSymbol symbol && Identifiers.isQualifiedName(symbol.text())) {
      return symbol.text();
    }
    throw expected(expr, "qualifizierter Name (" + what + ")");
  }

  static TypeRef type(SExpr expr) throws IrSyntaxException {
    if (expr instanceof SSymbol symbol) {
      return TypeRef.parse(symbol.text(), symbol.span());
    }
    throw expected(expr, "Typ");
  }

  static MemberRef member(SExpr expr) throws IrSyntaxException {
    if (expr instanceof SSymbol symbol) {
      return MemberRef.parse(symbol.text(), symbol.span());
    }
    throw expected(expr, "Member-Referenz");
  }

  static String string(SExpr expr, String what) throws IrSyntaxException {
    if (expr instanceof SString string) {
      return string.value();
    }
    throw expected(expr, "String (" + what + ")");
  }

  static SList list(SExpr expr, String what) throws IrSyntaxException {
    if (expr instanceof SList list) {
      return list;
    }
    throw expected(expr, "Liste (" + what + ")");
  }

  /** {@code ( {type} )}. */
  static List<TypeRef> typeList(SExpr expr, String what) throws IrSyntaxException {
    SList list = list(expr, what);
    List<TypeRef> result = new ArrayList<>();
    for (SExpr item : list.items()) {
      result.add(type(item));
    }
    return result;
  }

  /** {@code ( {ident} )}. */
  static List<String> identList(SExpr expr, String what) throws IrSyntaxException {
    SList list = list(expr, what);
    List<String> result = new ArrayList<>();
    for (SExpr item : list.items()) {
      result.add(ident(item, what));
    }
    return result;
  }

  /** {@code :mods ( {ident} )}, leer, wenn {@code expr} {@code null} ist. */
  static List<Modifier> modifiers(SExpr expr) throws IrSyntaxException {
    List<Modifier> result = new ArrayList<>();
    if (expr == null) {
      return result;
    }
    SList list = list(expr, ":mods");
    for (SExpr item : list.items()) {
      String name = ident(item, "Modifier");
      Modifier modifier = Modifier.fromSpelling(name);
      if (modifier == null) {
        throw new IrSyntaxException(item.span(), "unbekannter Modifier '" + name + "'");
      }
      result.add(modifier);
    }
    return result;
  }

  static IrSyntaxException expected(SExpr found, String what) {
    return new IrSyntaxException(found.span(), what + " erwartet, gefunden " + describe(found));
  }

  /** Kurzbeschreibung eines Elements für Fehlermeldungen. */
  static String describe(SExpr expr) {
    return switch (expr) {
      case SList list -> list.items().isEmpty() ? "leere Liste" : "Liste";
      case SSymbol symbol -> "'" + symbol.text() + "'";
      case SKeyword keyword -> "Keyword ':" + keyword.name() + "'";
      case SString string -> "String";
      case SNumber number -> "Zahl '" + number.text() + "'";
      case SBareInteger number -> "Zahl ohne Typsuffix '" + number.text() + "'";
    };
  }
}
