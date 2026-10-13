package io.github.localsoul.port.it;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.runtime.system.ArgumentOutOfRangeException;
import io.github.localsoul.runtime.system.Func0;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

/**
 * M3a.3: Fixtures mit BCL-Nutzung (C# → IR → Java → Lauf gegen {@code runtime-system}).
 * Erwartungswerte folgen aus der .NET-Semantik der C#-Quellen in {@code testdata/unity-sources},
 * nicht aus der Java-Implementierung.
 */
class RuntimeSystemEndToEndTest {

  private static ClassLoader classes;

  @BeforeAll
  static void compile() throws Exception {
    classes = GeneratedFixtures.classes();
  }

  private static Object create(String type) throws Exception {
    return classes.loadClass("gen.fixtures." + type).getDeclaredConstructor().newInstance();
  }

  /** Ruft eine Methode auf und gibt eine geworfene Exception unverpackt weiter. */
  private static Object call(Object target, String name, Class<?>[] types, Object... args)
      throws Throwable {
    Method method = target.getClass().getMethod(name, types);
    try {
      return method.invoke(target, args);
    } catch (InvocationTargetException e) {
      throw e.getCause();
    }
  }

  private static Object call(Object target, String name) throws Throwable {
    return call(target, name, new Class<?>[0]);
  }

  @Test
  void inventoryUsesListIndexerGenericBoxAndFunc() throws Throwable {
    Object inventory = create("Inventory");
    call(inventory, "fill", new Class<?>[] {int.class}, 5);
    // [0,1,2,3,4] → items[0] = 42 → RemoveAt(4) → [42,1,2,3]
    assertEquals(48, call(inventory, "total"));
    assertEquals("voll", call(inventory, "name"));
    Func0<Integer> seven = () -> 7;
    call(inventory, "setCounter", new Class<?>[] {Func0.class}, seven);
    assertEquals(7, call(inventory, "count"));
  }

  @Test
  void listIndexerOnEmptyListThrowsLikeDotNet() throws Throwable {
    Object inventory = create("Inventory");
    // Fill(0): leere Liste, items[0] = 42 wirft ArgumentOutOfRangeException.
    assertThrows(
        ArgumentOutOfRangeException.class,
        () -> call(inventory, "fill", new Class<?>[] {int.class}, 0));
  }

  @Test
  void stringComparisonIsOrdinalAndNullSafe() throws Throwable {
    Object flow = create("ControlFlow");
    Class<?>[] types = {String.class};
    assertEquals(3, call(flow, "score", types, "A"));
    assertEquals(2, call(flow, "score", types, new String("B")));
    assertEquals(0, call(flow, "score", types, "a"));
    assertEquals(0, call(flow, "score", types, (Object) null));
  }

  @Test
  void dictionaryKeepsDotNetOrderAfterRemoveAndAdd() throws Throwable {
    Object ledger = create("Ledger");
    // .NET: "d" füllt die durch Remove("b") frei gewordene Stelle.
    assertEquals("a=1;d=4;c=3;", call(ledger, "order"));
    assertEquals(1, call(ledger, "lookup", new Class<?>[] {String.class}, "a"));
    assertEquals(-1, call(ledger, "lookup", new Class<?>[] {String.class}, "fehlt"));
  }

  @Test
  void nullAccessIsCaughtAsNullReferenceException() throws Throwable {
    Object ledger = create("Ledger");
    Class<?> type = ledger.getClass();
    Class<?>[] types = {type};
    assertEquals(
        "try;nre:Object reference not set to an instance of an object.;finally",
        call(ledger, "guard", types, (Object) null));
    assertEquals("try;ok;finally", call(ledger, "guard", types, create("Ledger")));
  }

  @Test
  void thrownExceptionIsCaughtAsSystemException() throws Throwable {
    Object ledger = create("Ledger");
    assertEquals("negativ", call(ledger, "check", new Class<?>[] {int.class}, -1));
    assertEquals("ok", call(ledger, "check", new Class<?>[] {int.class}, 1));
  }

  @Test
  void floatsInConcatenationUseMonoFormat() throws Throwable {
    Object ledger = create("Ledger");
    Class<?>[] types = {float.class};
    assertEquals("v=0.3333333", call(ledger, "format", types, 1f / 3f));
    assertEquals("v=5", call(ledger, "format", types, 5f));
    assertEquals("v=1E-05", call(ledger, "format", types, 0.00001f));
  }

  @Test
  void bclFixturesHaveNoGeneratorTodos() throws Exception {
    for (var todo : GeneratedFixtures.generated().todos()) {
      String symbol = todo.symbol();
      assertTrue(!symbol.startsWith("Inventory") && !symbol.startsWith("Ledger"), todo.toString());
    }
  }
}
