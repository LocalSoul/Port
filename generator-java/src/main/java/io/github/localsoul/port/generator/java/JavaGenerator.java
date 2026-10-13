package io.github.localsoul.port.generator.java;

import io.github.localsoul.port.ir.ast.ClassDecl;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.ast.TypeDecl;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Erzeugt Java aus IR ({@code docs/modules/generator-java.md}).
 *
 * <ul>
 *   <li>Je IR-Klasse {@code FooBase.java} (alle generierten Member) und ein dünnes {@code
 *       Foo.java}, außer {@code manual/Foo.java} existiert ({@link
 *       GeneratorConfig#manualClasses()}) oder die Klasse braucht Handarbeit ({@code raw}/{@code
 *       :manual} → abstrakte Methode).
 *   <li>Nicht erzeugbare Konstrukte: Rumpf {@code throw new UnsupportedOperationException(
 *       "PORT-TODO: ...")} und ein {@link Todo}. Andere Typarten als Klassen kommen nach M3 (Todo).
 *   <li>Deterministisch: Typen nach Name, Member in kanonischer Reihenfolge, Importe sortiert.
 * </ul>
 */
public final class JavaGenerator {

  private final GeneratorConfig config;

  /**
   * Erzeugt den Generator.
   *
   * @param config Konfiguration
   */
  public JavaGenerator(GeneratorConfig config) {
    this.config = config;
  }

  /**
   * Erzeugt Java für alle Typen der Dateien. Typen aus allen Dateien gelten als eigene Typen.
   *
   * @param irFiles gelesene IR-Dateien eines Projekts
   * @return Dateien und offene Stellen, beide sortiert
   */
  public GenerationResult generate(List<IrFile> irFiles) {
    Map<String, TypeDecl> ownTypes = new TreeMap<>();
    for (IrFile file : irFiles) {
      for (TypeDecl type : file.types()) {
        ownTypes.put(type.name(), type);
      }
    }
    List<GeneratedFile> files = new ArrayList<>();
    List<Todo> todos = new ArrayList<>();
    ClassEmitter emitter = new ClassEmitter(config, ownTypes, files, todos);
    for (TypeDecl type : ownTypes.values()) {
      if (type instanceof ClassDecl clazz) {
        emitter.emit(clazz);
      } else {
        todos.add(
            new Todo(type.name(), type.getClass().getSimpleName() + " wird in M3 nicht erzeugt"));
      }
    }
    files.sort(Comparator.comparing(GeneratedFile::path));
    todos.sort(Comparator.comparing(Todo::symbol).thenComparing(Todo::reason));
    return new GenerationResult(files, todos);
  }
}
