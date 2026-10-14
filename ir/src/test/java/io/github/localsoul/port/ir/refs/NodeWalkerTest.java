package io.github.localsoul.port.ir.refs;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.localsoul.port.ir.ast.Stmt;
import io.github.localsoul.port.ir.ast.YieldReturnStmt;
import io.github.localsoul.port.ir.reader.IrReader;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Kontext des Durchlaufs: Symbol, try mit finally, Lambda. */
class NodeWalkerTest {

  private static final String IR =
      """
      (ir :version 1 :assembly "T")
      (class A
        (method M void ()
          (block
            (yield-return 1i32)
            (try (block (yield-return 2i32)) (finally (block (yield-return 3i32))))
            (try (block (yield-return 4i32)) (catch e System.Exception (block (yield-return 5i32))))
            (expr (lambda () (block (yield-return 6i32)))))))
      """;

  @Test
  void reportsTryWithFinallyAndLambdaContext() throws Exception {
    List<String> seen = new ArrayList<>();
    NodeWalker walker =
        new NodeWalker() {
          @Override
          protected void onStmt(Stmt stmt) {
            if (stmt instanceof YieldReturnStmt yield) {
              seen.add(
                  yield.value().span().line()
                      + ":"
                      + symbol()
                      + ":"
                      + insideTryWithFinally()
                      + ":"
                      + insideLambda()
                      + ":"
                      + (member() != null));
            }
          }
        };
    walker.walk(new IrReader().read(IR, "t.ir"));
    assertEquals(
        List.of(
            "5:A::M#:false:false:true",
            "6:A::M#:true:false:true",
            "6:A::M#:false:false:true",
            "7:A::M#:false:false:true",
            "7:A::M#:false:false:true",
            "8:A::M#:false:true:true"),
        seen);
  }
}
