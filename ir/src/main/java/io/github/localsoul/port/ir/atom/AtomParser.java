package io.github.localsoul.port.ir.atom;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.SourceSpan;
import java.util.ArrayList;
import java.util.List;

/**
 * Rekursiver Abstiegsparser für die Untersyntax von Typ- und Member-Atomen.
 *
 * <p>Fehlerpositionen sind die Position des Atoms plus der Versatz der Fehlerstelle im Atom. Atome
 * enthalten keine Zeilenumbrüche, deshalb bleibt die Zeile gleich.
 */
final class AtomParser {

  private final String text;
  private final SourceSpan atomStart;
  private int pos;

  AtomParser(String text, SourceSpan atomStart) {
    this.text = text;
    this.atomStart = atomStart;
  }

  /** {@code type} bis zum Ende des Atoms. */
  TypeRef parseWholeType() throws IrSyntaxException {
    TypeRef type = parseType();
    expectEnd("Typ");
    return type;
  }

  /** {@code mref := qname "::" name [ "<" types ">" ] [ "#" [ types ] ]} bis zum Ende des Atoms. */
  MemberRef parseWholeMember() throws IrSyntaxException {
    String declaringType = parseQualifiedName("Typname");
    if (!text.startsWith("::", pos)) {
      throw error(pos, "'::' erwartet");
    }
    pos += 2;
    String name = parseMemberName();
    List<TypeRef> genericArguments = List.of();
    if (peek('<')) {
      genericArguments = parseTypeArguments();
    }
    boolean hasParameterList = false;
    List<TypeRef> parameterTypes = new ArrayList<>();
    if (peek('#')) {
      pos++;
      hasParameterList = true;
      parseParameterTypes(parameterTypes);
    }
    expectEnd("Member-Referenz");
    return new MemberRef(declaringType, name, genericArguments, hasParameterList, parameterTypes);
  }

  /** {@code (prim | qname [ "<" type { "," type } ">" ]) { suffix }}. */
  private TypeRef parseType() throws IrSyntaxException {
    String name = parseQualifiedName("Typname");
    Primitive primitive = Primitive.fromSpelling(name);
    TypeRef type;
    if (primitive != null) {
      if (peek('<')) {
        throw error(pos, "primitiver Typ '" + name + "' hat keine Typargumente");
      }
      type = new PrimitiveType(primitive);
    } else {
      List<TypeRef> typeArguments = List.of();
      if (peek('<')) {
        typeArguments = parseTypeArguments();
      }
      type = new NamedType(name, typeArguments);
    }
    return parseSuffixes(type);
  }

  /** {@code { "[" { "," } "]" | "?" | "&" }}, wobei {@code &} das letzte Suffix sein muss. */
  private TypeRef parseSuffixes(TypeRef base) throws IrSyntaxException {
    TypeRef type = base;
    while (peek('[') || peek('?') || peek('&')) {
      if (type instanceof ByRefType) {
        throw error(pos, "nach '&' ist kein weiteres Suffix erlaubt");
      }
      char c = text.charAt(pos);
      if (c == '[') {
        type = parseArraySuffix(type);
      } else if (c == '?') {
        if (type instanceof NullableType) {
          throw error(pos, "doppeltes '?' ist nicht erlaubt");
        }
        pos++;
        type = new NullableType(type);
      } else {
        pos++;
        type = new ByRefType(type);
      }
    }
    return type;
  }

  private TypeRef parseArraySuffix(TypeRef elementType) throws IrSyntaxException {
    int open = pos;
    pos++;
    int rank = 1;
    while (peek(',')) {
      rank++;
      pos++;
    }
    if (!peek(']')) {
      throw error(open, "Array-Suffix '[' nicht geschlossen");
    }
    pos++;
    return new ArrayType(elementType, rank);
  }

  /** {@code "<" type { "," type } ">"}, mindestens ein Typ. */
  private List<TypeRef> parseTypeArguments() throws IrSyntaxException {
    int open = pos;
    pos++;
    if (peek('>')) {
      throw error(pos, "leere Typargumentliste '<>'");
    }
    List<TypeRef> arguments = new ArrayList<>();
    while (true) {
      arguments.add(parseType());
      if (peek(',')) {
        pos++;
      } else if (peek('>')) {
        pos++;
        return arguments;
      } else if (pos >= text.length()) {
        throw error(open, "Typargumentliste '<' nicht geschlossen");
      } else {
        throw error(pos, "',' oder '>' erwartet");
      }
    }
  }

  /** {@code [ type { "," type } ]} nach {@code #} bis zum Ende des Atoms. */
  private void parseParameterTypes(List<TypeRef> into) throws IrSyntaxException {
    if (pos >= text.length()) {
      return;
    }
    into.add(parseType());
    while (peek(',')) {
      pos++;
      into.add(parseType());
    }
  }

  /** {@code ident | ".ctor" | ".cctor"}. */
  private String parseMemberName() throws IrSyntaxException {
    if (peek('.')) {
      int start = pos;
      pos++;
      String ident = readIdent();
      if (ident.equals("ctor") || ident.equals("cctor")) {
        return "." + ident;
      }
      throw error(start, "Member-Name erwartet (Bezeichner, .ctor oder .cctor)");
    }
    String ident = readIdent();
    if (ident.isEmpty()) {
      throw error(pos, "Member-Name erwartet (Bezeichner, .ctor oder .cctor)");
    }
    return ident;
  }

  /** {@code ident { "." ident }}. */
  private String parseQualifiedName(String what) throws IrSyntaxException {
    int start = pos;
    String first = readIdent();
    if (first.isEmpty()) {
      throw error(pos, what + " erwartet");
    }
    while (peek('.')) {
      pos++;
      String next = readIdent();
      if (next.isEmpty()) {
        throw error(pos, "Bezeichner nach '.' erwartet");
      }
    }
    return text.substring(start, pos);
  }

  /** Liest {@code [A-Za-z_][A-Za-z0-9_]*}, liefert einen leeren String, wenn keiner beginnt. */
  private String readIdent() {
    int start = pos;
    while (pos < text.length()) {
      char c = text.charAt(pos);
      boolean letter = (c >= 'A' && c <= 'Z') || (c >= 'a' && c <= 'z') || c == '_';
      boolean digit = c >= '0' && c <= '9';
      if (letter || (digit && pos > start)) {
        pos++;
      } else {
        break;
      }
    }
    return text.substring(start, pos);
  }

  private void expectEnd(String what) throws IrSyntaxException {
    if (pos < text.length()) {
      String rest = text.substring(pos);
      throw error(pos, "unerwartetes '" + rest + "' nach " + what);
    }
  }

  private boolean peek(char expected) {
    return pos < text.length() && text.charAt(pos) == expected;
  }

  private IrSyntaxException error(int offset, String reason) {
    int columnOffset = text.codePointCount(0, Math.min(offset, text.length()));
    SourceSpan span =
        new SourceSpan(atomStart.file(), atomStart.line(), atomStart.column() + columnOffset);
    return new IrSyntaxException(span, reason + " in '" + text + "'");
  }

  /** Hängt {@code open t1,t2 close} an, nichts bei leerer Liste. */
  static void appendTypeList(StringBuilder out, char open, List<TypeRef> types, char close) {
    if (types.isEmpty()) {
      return;
    }
    out.append(open);
    for (int i = 0; i < types.size(); i++) {
      if (i > 0) {
        out.append(',');
      }
      out.append(types.get(i));
    }
    out.append(close);
  }
}
