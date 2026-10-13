package io.github.localsoul.port.generator.java;

import java.util.Set;

/** Java-Namen für IR-Bezeichner (Konvention aus {@code docs/modules/generator-java.md}). */
final class JavaNames {

  private static final Set<String> KEYWORDS =
      Set.of(
          "abstract",
          "assert",
          "boolean",
          "break",
          "byte",
          "case",
          "catch",
          "char",
          "class",
          "const",
          "continue",
          "default",
          "do",
          "double",
          "else",
          "enum",
          "extends",
          "final",
          "finally",
          "float",
          "for",
          "goto",
          "if",
          "implements",
          "import",
          "instanceof",
          "int",
          "interface",
          "long",
          "native",
          "new",
          "package",
          "private",
          "protected",
          "public",
          "return",
          "short",
          "static",
          "strictfp",
          "super",
          "switch",
          "synchronized",
          "this",
          "throw",
          "throws",
          "transient",
          "try",
          "void",
          "volatile",
          "while",
          "true",
          "false",
          "null",
          "var",
          "record",
          "yield",
          "sealed",
          "permits",
          "_");

  private JavaNames() {}

  /** Bezeichner, Java-Schlüsselwörter bekommen ein {@code _} angehängt. */
  static String ident(String name) {
    return KEYWORDS.contains(name) ? name + "_" : name;
  }

  /** Methoden und Properties: erster Buchstabe klein ({@code Update} → {@code update}). */
  static String member(String name) {
    if (name.isEmpty() || !Character.isUpperCase(name.charAt(0))) {
      return ident(name);
    }
    return ident(Character.toLowerCase(name.charAt(0)) + name.substring(1));
  }

  /** Paketname aus Basispaket und IR-Namespace (Kleinbuchstaben). */
  static String packageOf(String basePackage, String irQualifiedName) {
    int dot = irQualifiedName.lastIndexOf('.');
    if (dot < 0) {
      return basePackage;
    }
    String namespace = irQualifiedName.substring(0, dot).toLowerCase(java.util.Locale.ROOT);
    StringBuilder out = new StringBuilder(basePackage);
    for (String part : namespace.split("\\.")) {
      out.append('.').append(ident(part));
    }
    return out.toString();
  }

  /** Einfacher Name eines qualifizierten IR-Typs. */
  static String simpleName(String irQualifiedName) {
    int dot = irQualifiedName.lastIndexOf('.');
    return dot < 0 ? irQualifiedName : irQualifiedName.substring(dot + 1);
  }
}
