package io.github.localsoul.port.generator.java;

import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.TypeDecl;
import io.github.localsoul.port.ir.atom.ArrayType;
import io.github.localsoul.port.ir.atom.ByRefType;
import io.github.localsoul.port.ir.atom.NamedType;
import io.github.localsoul.port.ir.atom.NullableType;
import io.github.localsoul.port.ir.atom.PrimitiveType;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * Typauflösung und Importe für eine erzeugte Datei. Eigene Typen (aus den übergebenen IR-Dateien)
 * liegen im Zielpaket, externe Typen kommen aus dem Mapping. Primitive als Typargument werden
 * geboxt ({@code List<int>} → {@code List<Integer>}, {@code docs/type-mapping.md}).
 */
final class TypeContext {

  private final GeneratorConfig config;
  private final Map<String, TypeDecl> ownTypes;
  private final String filePackage;
  private final Set<String> typeParameters = new TreeSet<>();
  // Einfacher Name → importierter voll qualifizierter Name; nur für Lookups.
  private final Map<String, String> imports = new TreeMap<>();

  TypeContext(GeneratorConfig config, Map<String, TypeDecl> ownTypes, String filePackage) {
    this.config = config;
    this.ownTypes = ownTypes;
    this.filePackage = filePackage;
  }

  GeneratorConfig config() {
    return config;
  }

  Mapping mapping() {
    return config.mapping();
  }

  boolean isOwnType(String irName) {
    return ownTypes.containsKey(irName);
  }

  TypeDecl ownType(String irName) {
    return ownTypes.get(irName);
  }

  /** Eigener Typ, der als Klasse erzeugt wird. */
  boolean isGeneratedClass(String irName) {
    return ownTypes.get(irName) instanceof ClassDecl;
  }

  void addTypeParameters(Iterable<String> names) {
    for (String name : names) {
      typeParameters.add(name);
    }
  }

  /** Setzt die sichtbaren Typparameter zurück (nach einer generischen Methode). */
  Set<String> typeParameters() {
    return new HashSet<>(typeParameters);
  }

  void restoreTypeParameters(Set<String> saved) {
    typeParameters.clear();
    typeParameters.addAll(saved);
  }

  /**
   * Sicherung der Importe vor einem Member, damit ein Todo keine ungenutzten Importe hinterlässt.
   */
  Map<String, String> saveImports() {
    return new TreeMap<>(imports);
  }

  void restoreImports(Map<String, String> saved) {
    imports.clear();
    imports.putAll(saved);
  }

  /** Sortierte Importe für den Dateikopf. */
  Set<String> imports() {
    return new TreeSet<>(imports.values());
  }

  /** Java-Typ für eine IR-Typreferenz. */
  String javaType(TypeRef type) {
    return javaType(type, false);
  }

  /** Java-Typ; {@code boxed} für Typargumente (Primitive als Wrapper). */
  String javaType(TypeRef type, boolean boxed) {
    return switch (type) {
      case PrimitiveType p -> primitive(p, boxed);
      case ArrayType a -> array(a);
      case NamedType n -> named(n);
      case NullableType n -> throw new TodoException("Nullable-Typ " + n + " (nach M3)");
      case ByRefType b ->
          throw new TodoException("ref/out-Typ " + b + " (Java hat keine Referenzparameter)");
    };
  }

  private String array(ArrayType array) {
    if (array.rank() != 1) {
      throw new TodoException("mehrdimensionales Array " + array + " (SM008)");
    }
    return javaType(array.elementType()) + "[]";
  }

  private static String primitive(PrimitiveType type, boolean boxed) {
    return switch (type.primitive()) {
      case BOOL -> boxed ? "Boolean" : "boolean";
      case CHAR -> boxed ? "Character" : "char";
      case STRING -> "String";
      case OBJECT -> "Object";
      case VOID -> boxed ? "Void" : "void";
      case I8 -> boxed ? "Byte" : "byte";
      case I16 -> boxed ? "Short" : "short";
      case I32 -> boxed ? "Integer" : "int";
      case I64 -> boxed ? "Long" : "long";
      case F32 -> boxed ? "Float" : "float";
      case F64 -> boxed ? "Double" : "double";
      case U8, U16, U32, U64 ->
          throw new TodoException("vorzeichenloser Typ " + type + " (ADR-019, SM002)");
      case PTR -> throw new TodoException("nativer Zeiger (ab M10b)");
    };
  }

  private String named(NamedType type) {
    String name = type.qualifiedName();
    if (type.typeArguments().isEmpty() && typeParameters.contains(name)) {
      return name;
    }
    String base = className(name);
    if (type.typeArguments().isEmpty()) {
      return base;
    }
    StringBuilder out = new StringBuilder(base).append('<');
    for (int i = 0; i < type.typeArguments().size(); i++) {
      if (i > 0) {
        out.append(", ");
      }
      out.append(javaType(type.typeArguments().get(i), true));
    }
    return out.append('>').toString();
  }

  /** Klassenname eines IR-Typs (ohne Typargumente), mit Import. */
  String className(String irName) {
    if (ownTypes.containsKey(irName)) {
      if (!(ownTypes.get(irName) instanceof ClassDecl)) {
        throw new TodoException(
            "eigener Typ " + irName + " wird in M3 nicht erzeugt (nur Klassen)");
      }
      String javaPackage = JavaNames.packageOf(config.basePackage(), irName);
      return reference(javaPackage, JavaNames.simpleName(irName));
    }
    String mapped = config.mapping().javaType(irName);
    if (mapped == null) {
      throw new TodoException("externer Typ " + irName + " ohne Mapping");
    }
    int dot = mapped.lastIndexOf('.');
    return reference(mapped.substring(0, dot), mapped.substring(dot + 1));
  }

  /** Einfacher Name mit Import, bei Namenskonflikt voll qualifiziert. */
  String reference(String javaPackage, String simpleName) {
    if (javaPackage.equals(filePackage)) {
      return simpleName;
    }
    String qualified = javaPackage + "." + simpleName;
    String existing = imports.get(simpleName);
    if (existing == null) {
      imports.put(simpleName, qualified);
      return simpleName;
    }
    return existing.equals(qualified) ? simpleName : qualified;
  }
}
