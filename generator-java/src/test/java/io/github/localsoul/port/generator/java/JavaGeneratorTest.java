package io.github.localsoul.port.generator.java;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.reader.IrReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Einzelne Erzeugungsregeln an kleinen IR-Ausschnitten. */
class JavaGeneratorTest {

  private static final String HEADER = "(ir :version 1 :assembly \"T\")\n";
  private static final String MAPPING =
      "Ext.Base\tx.ext.Base\ttype\n"
          + "Ext.Base::Tick#\ttick\tmessage\n"
          + "Ext.Vec\tx.ext.Vec\ttype\n"
          + "Ext.Vec::op_Multiply#Ext.Vec,f32\tmul\toperator\n"
          + "Ext.Vec::.ctor#f32\tVec\tconstructor\n"
          + "Ext.Vec::Describe#i32\tdescribe\thelper\n"
          + "Ext.Fn/1\tx.ext.Fn0\ttype\n"
          + "Ext.Fn/2\tx.ext.Fn1\ttype\n"
          + "Ext.Fn::Invoke#T\tinvoke\tmethod\n"
          + "System.Exception\tx.sys.SystemException\ttype\n"
          + "System.InvalidOperationException\tx.sys.InvalidOperationException\ttype\n"
          + "System.InvalidOperationException::.ctor#string\tInvalidOperationException\tconstructor\n"
          + "System.NullReferenceException\tx.sys.NullReferenceException\ttype\n"
          + "Ext.V2\tx.ext.V2\tstruct\n"
          + "Ext.V2::x\tx\tgetter\n"
          + "Ext.V2::Normalize#\tnormalized\tmutator\n"
          + "Ext.T\tx.ext.T\ttype\n"
          + "Ext.T::pos\tpos\tgetter\n"
          + "Ext.T::pos:set\tpos\tsetter\n"
          + "Ext.Screen\tx.ext.Screen\ttype\n"
          + "Ext.Screen::size\tsize\tstatic-getter\n"
          + "Ext.Screen::size:set\tsize\tstatic-setter\n"
          + "Ext.Key\tx.ext.Key\ttype\n"
          + "Ext.Key::A\tA\tconstant\n"
          + "Ext.Go\tx.ext.Go\ttype\n"
          + "Ext.Go::Add<T>#\tadd\tfactory\n"
          + "System.Array\tx.rt.RuntimeArrays\ttype\n"
          + "System.Array::Fill#T[],T\tfilled\tstatic-method\n";

  private static GenerationResult generate(String body, Set<String> manual) throws Exception {
    GeneratorConfig config = new GeneratorConfig("p", Mapping.parse(MAPPING), manual);
    return new JavaGenerator(config).generate(List.of(new IrReader().read(HEADER + body, "t.ir")));
  }

  private static GenerationResult generate(String body) throws Exception {
    return generate(body, Set.of());
  }

  private static String file(GenerationResult result, String path) {
    for (GeneratedFile file : result.files()) {
      if (file.path().equals(path)) {
        return file.content();
      }
    }
    throw new AssertionError("Datei fehlt: " + path + " in " + paths(result));
  }

  private static List<String> paths(GenerationResult result) {
    List<String> paths = new ArrayList<>();
    for (GeneratedFile file : result.files()) {
      paths.add(file.path());
    }
    return paths;
  }

  private static String methodBody(String body) throws Exception {
    return file(
        generate("(class A (method M void ((a i32) (b i32) (c bool)) (block " + body + ")))"),
        "p/ABase.java");
  }

  @Test
  void setsParenthesesOnlyWhereJavaPrecedenceNeedsThem() throws Exception {
    String text =
        methodBody(
            "(local x i32 (* (+ a b) b)) (local y i32 (+ a (* a b))) (local z i32 (- a (- b a)))"
                + " (local w bool (not (&& c (> a b)))) (local v i32 (neg (neg a)))");
    assertTrue(text.contains("int x = (a + b) * b;"), text);
    assertTrue(text.contains("int y = a + a * b;"), text);
    assertTrue(text.contains("int z = a - (b - a);"), text);
    assertTrue(text.contains("boolean w = !(c && a > b);"), text);
    assertTrue(text.contains("int v = -(-a);"), text);
  }

  @Test
  void writesLiteralsWithJavaTypes() throws Exception {
    String text =
        methodBody(
            "(local a1 i8 -5i8) (local a2 i16 7i16) (local a3 i64 0x7FFFFFFFFFFFFFFFi64)"
                + " (local a4 f64 1.5e-3f64) (local a5 f64 2f64) (local a6 char (char 97))"
                + " (local a7 char (char 10)) (local a8 string \"tab\\t\\u0001\\\"\")");
    assertTrue(text.contains("byte a1 = (byte) -5;"), text);
    assertTrue(text.contains("short a2 = (short) 7;"), text);
    assertTrue(text.contains("long a3 = 0x7FFFFFFFFFFFFFFFL;"), text);
    assertTrue(text.contains("double a4 = 1.5e-3;"), text);
    assertTrue(text.contains("double a5 = 2d;"), text);
    assertTrue(text.contains("char a6 = 'a';"), text);
    assertTrue(text.contains("char a7 = (char) 10;"), text);
    assertTrue(text.contains("String a8 = \"tab\\t\\001\\\"\";"), text);
  }

  @Test
  void unsignedTypesBecomeTodo() throws Exception {
    GenerationResult result = generate("(class A (method M void () (block (local x u32 1u32))))");
    String text = file(result, "p/ABase.java");
    assertTrue(text.contains("PORT-TODO: A::M#: vorzeichenloser Typ u32 (ADR-019, SM002)"), text);
    assertEquals(
        List.of(new Todo("A::M#", "vorzeichenloser Typ u32 (ADR-019, SM002)")), result.todos());
  }

  @Test
  void overridesMessagesFromMappingAndUsesOperators() throws Exception {
    String text =
        file(
            generate(
                "(class A :base Ext.Base (method Tick void () :mods (private) (block (local v Ext.Vec"
                    + " (scall Ext.Vec::op_Multiply#Ext.Vec,f32 (new Ext.Vec::.ctor#f32 1.0f32) 2.0f32))))"
                    + " (method Other void () :mods (private) (return)))"),
            "p/ABase.java");
    assertTrue(text.contains("import x.ext.Base;\nimport x.ext.Vec;"), text);
    assertTrue(text.contains("public abstract class ABase extends Base {"), text);
    assertTrue(text.contains("  @Override\n  protected void tick() {"), text);
    assertTrue(text.contains("Vec v = Vec.mul(new Vec(1.0f), 2.0f);"), text);
    assertTrue(text.contains("  void other() {"), text);
  }

  @Test
  void thinClassRepeatsConstructorsAndRespectsManualClasses() throws Exception {
    String ir =
        "(class B :mods (public sealed) (ctor ((x i32)) :mods (public) :base-call () (block))"
            + " (ctor () :mods (static) (block)))";
    GenerationResult result = generate(ir);
    assertEquals(
        "package p;\n\n/** Generiert, solange es keine Handarbeit in {@code manual/B.java} gibt. */\n"
            + "public final class B extends BBase {\n"
            + "  public B(int x) {\n    super(x);\n  }\n}\n",
        file(result, "p/B.java"));
    String base = file(result, "p/BBase.java");
    assertTrue(base.contains("  protected BBase(int x) {}"), base);
    assertTrue(base.contains("  static {}"), base);
    assertEquals(List.of("p/BBase.java"), paths(generate(ir, Set.of("B"))));
  }

  @Test
  void rawMakesMethodAbstractAndRequiresManualClass() throws Exception {
    GenerationResult result = generate("(class C (method M i32 () :mods (public) (raw cs \"x\")))");
    assertEquals(List.of("p/CBase.java"), paths(result));
    assertTrue(file(result, "p/CBase.java").contains("public abstract int m();"));
    assertTrue(
        result
            .todos()
            .contains(new Todo("C", "Klasse braucht Handarbeit: manual/C.java extends CBase")));
  }

  @Test
  void propertyAssignmentsBecomeSetterCalls() throws Exception {
    String text =
        file(
            generate(
                "(class D (prop P i32 :mods (public) :get (return 1i32) :set (block))"
                    + " (method M void () (block (expr (set (prop this D::P) 3i32))"
                    + " (expr (set-op + (prop this D::P) 2i32)))))"),
            "p/DBase.java");
    assertTrue(text.contains("this.p(3);"), text);
    assertTrue(text.contains("this.p(this.p() + 2);"), text);
    assertTrue(text.contains("public void p(int value) {}"), text);
  }

  @Test
  void namespacesBecomeLowerCasePackagesAndKeywordsAreEscaped() throws Exception {
    GenerationResult result = generate("(class Game.Logic.E (field new i32 :mods (public)))");
    assertTrue(file(result, "p/game/logic/EBase.java").contains("public int new_;"));
  }

  @Test
  void otherTypeKindsAreReportedAsTodo() throws Exception {
    GenerationResult result = generate("(enum F :underlying i32 (A 0i32))");
    assertEquals(List.of(new Todo("F", "EnumDecl wird in M3 nicht erzeugt")), result.todos());
  }

  @Test
  void rejectsMalformedMapping() {
    String[] bad = {"a\tb", "a\tb\tunbekannt", "a\tb\ttype\na\tc\ttype"};
    for (String tsv : bad) {
      try {
        Mapping.parse(tsv);
        throw new AssertionError("erwartet MappingException für " + tsv);
      } catch (MappingException e) {
        assertTrue(e.getMessage().startsWith("Zeile "), e.getMessage());
      }
    }
  }

  @Test
  void unusedImportsOfFailedMembersAreDropped() throws Exception {
    String text =
        file(
            generate(
                "(class G (method M void () (block (local v Ext.Vec (new Ext.Vec::.ctor#f32 1.0f32))"
                    + " (local x u8 1u8))))"),
            "p/GBase.java");
    assertTrue(!text.contains("import"), text);
  }

  @Test
  void typeRowsWithArityPickTheMatchingClass() throws Exception {
    String text =
        file(
            generate(
                "(class F (field a Ext.Fn<i32>) (field b Ext.Fn<i32,string>)"
                    + " (method M string ((v i32)) (block (return (call (field this F::b)"
                    + " Ext.Fn<i32,string>::Invoke#T v)))))"),
            "p/FBase.java");
    assertTrue(text.contains("Fn0<Integer> a;"), text);
    assertTrue(text.contains("Fn1<Integer, String> b;"), text);
    assertTrue(text.contains("return this.b.invoke(v);"), text);
  }

  @Test
  void helperFormPassesReceiverAsFirstArgument() throws Exception {
    String text =
        file(
            generate(
                "(class H (method M string ((v Ext.Vec)) (block (return (call v"
                    + " Ext.Vec::Describe#i32 2i32)))))"),
            "p/HBase.java");
    assertTrue(text.contains("return Vec.describe(v, 2);"), text);
  }

  @Test
  void tryCatchFinallyTranslatesJavaExceptionsToDotNet() throws Exception {
    String text =
        methodBody(
            "(try (block (if c (block (throw (new System.InvalidOperationException::.ctor#string"
                + " \"x\")))))"
                + " (catch n System.NullReferenceException (block (return)))"
                + " (catch ex System.InvalidOperationException (block (throw ex)))"
                + " (catch e System.Exception (block (expr (set a 1i32))))"
                + " (finally (block (expr (set b 2i32)))))");
    String expected =
        """
            try {
              if (c) {
                throw new InvalidOperationException("x");
              }
            } catch (NullReferenceException | NullPointerException n$) {
              NullReferenceException n = NullReferenceException.fromJava(n$);
              return;
            } catch (InvalidOperationException ex) {
              throw ex;
            } catch (SystemException | NullPointerException | ArrayIndexOutOfBoundsException e$) {
              SystemException e = SystemException.fromJava(e$);
              a = 1;
            } finally {
              b = 2;
            }
        """;
    assertTrue(text.contains(expected), text);
    assertEquals(text, new com.google.googlejavaformat.java.Formatter().formatSource(text));
  }

  @Test
  void longMultiCatchIsWrappedLikeGoogleJavaFormat() throws Exception {
    String deep = "(try (block (return)) (catch e System.Exception (block (return))))";
    for (int i = 0; i < 6; i++) {
      deep = "(if c (block " + deep + "))";
    }
    String text = methodBody(deep);
    assertTrue(text.contains("| ArrayIndexOutOfBoundsException e$) {"), text);
    assertEquals(text, new com.google.googlejavaformat.java.Formatter().formatSource(text));
  }

  @Test
  void javaClassNameFollowsThePackageRule() {
    assertEquals("game.demo.Mover", JavaGenerator.javaClassName("game.demo", "Mover"));
    assertEquals(
        "game.demo.game.logic.Enemy", JavaGenerator.javaClassName("game.demo", "Game.Logic.Enemy"));
  }

  @Test
  void valueTypesSettersConstantsAndFactories() throws Exception {
    String text =
        file(
            generate(
                "(class A (field f Ext.V2)"
                    + " (method M void ((v Ext.V2) (t Ext.T) (g Ext.Go)) (block"
                    + " (expr (set (field v Ext.V2::x) 1.0f32))"
                    + " (expr (set-op * (field v Ext.V2::x) 2.0f32))"
                    + " (expr (post-inc (field v Ext.V2::x)))"
                    + " (expr (set (field (field this A::f) Ext.V2::x) 0.0f32))"
                    + " (expr (call v Ext.V2::Normalize#))"
                    + " (expr (set (prop t Ext.T::pos) v))"
                    + " (expr (set (sprop Ext.Screen::size) 3i32))"
                    + " (expr (set-op + (sprop Ext.Screen::size) 1i32))"
                    + " (local k Ext.Key (sfield Ext.Key::A))"
                    + " (local c Ext.T (call g Ext.Go::Add<Ext.T>#))"
                    + " (local arr Ext.V2[] (new-array Ext.V2 4i32))"
                    + " (local d Ext.V2 (default Ext.V2))"
                    + " (local e Ext.V2)"
                    + " (expr (set (field (index arr 0i32) Ext.V2::x) 3.0f32)))))"),
            "p/ABase.java");
    for (String expected :
        List.of(
            "v = v.withX(1.0f);",
            "v = v.withX(v.x() * 2.0f);",
            "v = v.withX(v.x() + 1);",
            "this.f = this.f.withX(0.0f);",
            "v = v.normalized();",
            "t.pos(v);",
            "Screen.size(3);",
            "Screen.size(Screen.size() + 1);",
            "Key k = Key.A;",
            "T c = g.add(T::new);",
            "V2[] arr = RuntimeArrays.filled(new V2[4], V2.DEFAULT);",
            "V2 d = V2.DEFAULT;",
            "V2 e = V2.DEFAULT;",
            "arr[0] = arr[0].withX(3.0f);")) {
      assertTrue(text.contains(expected), expected + "\n" + text);
    }
    assertEquals(text, new com.google.googlejavaformat.java.Formatter().formatSource(text));
  }

  @Test
  void genericKeyReplacesMethodTypeArguments() {
    assertEquals(
        "UnityEngine.GameObject::AddComponent<T>#",
        Mapping.genericKey("UnityEngine.GameObject::AddComponent<UnityEngine.SpriteRenderer>#", 1));
    assertEquals("A<i32>::M<T1,T2>#x", Mapping.genericKey("A<i32>::M<B,C<D,E>>#x", 2));
    assertEquals("A::M#", Mapping.genericKey("A::M#", 0));
  }

  @Test
  void mutatorAsExpressionIsTodo() throws Exception {
    GenerationResult result =
        generate(
            "(class A (method M Ext.V2 ((v Ext.V2)) (block (return (call v Ext.V2::Normalize#)))))");
    assertTrue(
        result.todos().get(0).reason().contains("nur als Statement"), result.todos().toString());
  }

  @Test
  void parsesIrSyntaxExceptionFree() throws IrSyntaxException {
    assertEquals(1, new IrReader().read(HEADER, "x.ir").header().version());
  }
}
