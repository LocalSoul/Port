package io.github.localsoul.port.it;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.FileObject;
import javax.tools.ForwardingJavaFileManager;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.StandardJavaFileManager;
import javax.tools.ToolProvider;

/** Kompiliert Java-Quelltext im Speicher gegen den Test-Klassenpfad und lädt die Klassen. */
final class InMemoryCompiler {

  private InMemoryCompiler() {}

  /**
   * Kompiliert und lädt.
   *
   * @param sources Pfad (z. B. {@code gen/fixtures/Mover.java}) → Quelltext
   * @return Klassenlader mit den kompilierten Klassen
   * @throws AssertionError mit allen Compilermeldungen, wenn die Kompilierung scheitert
   */
  static ClassLoader compile(Map<String, String> sources) {
    JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
    DiagnosticCollector<JavaFileObject> diagnostics = new DiagnosticCollector<>();
    StandardJavaFileManager standard = compiler.getStandardFileManager(diagnostics, null, null);
    Map<String, ByteArrayOutputStream> classes = new HashMap<>();
    JavaFileManager manager = new MemoryFileManager(standard, classes);
    List<JavaFileObject> units = new ArrayList<>();
    for (Map.Entry<String, String> source : sources.entrySet()) {
      units.add(new Source(source.getKey(), source.getValue()));
    }
    List<String> options =
        List.of(
            "-classpath",
            System.getProperty("java.class.path"),
            "-Xlint:none",
            "-encoding",
            "UTF-8");
    StringWriter output = new StringWriter();
    boolean ok = compiler.getTask(output, manager, diagnostics, options, null, units).call();
    if (!ok) {
      StringBuilder message = new StringBuilder("Kompilierung fehlgeschlagen:\n");
      for (Diagnostic<? extends JavaFileObject> diagnostic : diagnostics.getDiagnostics()) {
        message.append(diagnostic).append('\n');
      }
      throw new AssertionError(message.toString());
    }
    return new MemoryClassLoader(classes);
  }

  private static final class Source extends SimpleJavaFileObject {
    private final String code;

    Source(String path, String code) {
      super(URI.create("string:///" + path), Kind.SOURCE);
      this.code = code;
    }

    @Override
    public CharSequence getCharContent(boolean ignoreEncodingErrors) {
      return code;
    }
  }

  private static final class MemoryFileManager extends ForwardingJavaFileManager<JavaFileManager> {
    private final Map<String, ByteArrayOutputStream> classes;

    MemoryFileManager(JavaFileManager delegate, Map<String, ByteArrayOutputStream> classes) {
      super(delegate);
      this.classes = classes;
    }

    @Override
    public JavaFileObject getJavaFileForOutput(
        Location location, String className, JavaFileObject.Kind kind, FileObject sibling) {
      ByteArrayOutputStream bytes = new ByteArrayOutputStream();
      classes.put(className, bytes);
      return new SimpleJavaFileObject(
          URI.create("mem:///" + className.replace('.', '/') + kind.extension), kind) {
        @Override
        public OutputStream openOutputStream() {
          return bytes;
        }
      };
    }
  }

  private static final class MemoryClassLoader extends ClassLoader {
    private final Map<String, ByteArrayOutputStream> classes;

    MemoryClassLoader(Map<String, ByteArrayOutputStream> classes) {
      super(InMemoryCompiler.class.getClassLoader());
      this.classes = classes;
    }

    @Override
    protected Class<?> findClass(String name) throws ClassNotFoundException {
      ByteArrayOutputStream bytes = classes.get(name);
      if (bytes == null) {
        throw new ClassNotFoundException(name);
      }
      byte[] data = bytes.toByteArray();
      return defineClass(name, data, 0, data.length);
    }
  }
}
