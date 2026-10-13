package io.github.localsoul.port.engine.core.subsystems;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystems.input.InputScript;
import io.github.localsoul.port.engine.core.subsystems.input.ScriptedInput;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class ScriptedInputTest {

  private static CoreEngine engineWith(InputScript script) {
    return CoreEngine.builder().subsystem(new ScriptedInput(script)).build();
  }

  @Test
  void scriptedKeyIsVisibleFromEarlyOfItsFrame() {
    CoreEngine engine = engineWith(new InputScript().at(3).press("Jump").at(6).release("Jump"));
    List<String> log = new ArrayList<>();
    for (Phase phase : Phase.values()) {
      engine.register(
          phase,
          ctx -> {
            boolean pressed = ctx.input().wasPressed("Jump");
            boolean held = ctx.input().isHeld("Jump");
            boolean released = ctx.input().wasReleased("Jump");
            if (pressed || held || released) {
              log.add(
                  ctx.time().frame()
                      + " "
                      + ctx.phase()
                      + " p="
                      + pressed
                      + " h="
                      + held
                      + " r="
                      + released);
            }
          });
    }
    engine.step(8);
    List<String> earlyOnly = new ArrayList<>();
    for (String line : log) {
      if (line.contains("EARLY")) {
        earlyOnly.add(line);
      }
    }
    assertEquals(
        List.of(
            "3 EARLY p=true h=true r=false",
            "4 EARLY p=false h=true r=false",
            "5 EARLY p=false h=true r=false",
            "6 EARLY p=false h=false r=true"),
        earlyOnly);
    assertEquals(
        "3 UPDATE p=true h=true r=false",
        log.get(log.indexOf("3 EARLY p=true h=true r=false") + 2));
  }

  @Test
  void axisValuesPersistUntilChanged() {
    CoreEngine engine =
        engineWith(new InputScript().at(1).axis("Horizontal", 0.5f).at(3).axis("Horizontal", -1f));
    List<Float> values = new ArrayList<>();
    engine.register(Phase.UPDATE, ctx -> values.add(ctx.input().axis("Horizontal")));
    engine.step(5);
    assertEquals(List.of(0f, 0.5f, 0.5f, -1f, -1f), values);
  }

  @Test
  void repeatedPressAndReleaseWithoutHoldChangeNothing() {
    CoreEngine engine =
        engineWith(new InputScript().at(0).release("A").press("A").press("A").at(1).press("A"));
    List<String> log = new ArrayList<>();
    engine.register(
        Phase.UPDATE,
        ctx ->
            log.add(
                ctx.input().wasPressed("A")
                    + "/"
                    + ctx.input().wasReleased("A")
                    + "/"
                    + ctx.input().isHeld("A")));
    engine.step(2);
    assertEquals(List.of("true/false/true", "false/false/true"), log);
  }

  @Test
  void withoutInputSubsystemEverythingIsNeutral() {
    CoreEngine engine = CoreEngine.builder().build();
    List<String> log = new ArrayList<>();
    engine.register(
        Phase.UPDATE, ctx -> log.add(ctx.input().axis("X") + " " + ctx.input().isHeld("A")));
    engine.step(1);
    assertEquals(List.of("0.0 false"), log);
  }

  @Test
  void inputStateAppearsInDump() {
    CoreEngine engine = engineWith(new InputScript().at(0).press("B").axis("Y", 1f));
    engine.step(1);
    String dump = engine.world().dump();
    assertEquals(
        "world entities=1\nentity 0:0\n"
            + "  InputState InputState[axes={Y=1.0}, held=[B], pressed=[B], released=[]]\n",
        dump);
  }

  @Test
  void rejectsNegativeFrameInScript() {
    assertThrows(IllegalArgumentException.class, () -> new InputScript().at(-1));
  }
}
