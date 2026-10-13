package io.github.localsoul.adapter.unity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.math.Quat;
import io.github.localsoul.port.engine.api.math.Vec3;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.core.CoreEngine;
import io.github.localsoul.port.engine.core.subsystems.input.InputScript;
import io.github.localsoul.port.engine.core.subsystems.input.ScriptedInput;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Koordinatenkonvertierung Unity (links-händig) ↔ Core (rechts-händig), ADR-007. */
class CoordinateTest {

  private static io.github.localsoul.port.engine.api.component.Transform core(
      CoreEngine engine, GameObject go) {
    return engine
        .world()
        .get(go.entity(), io.github.localsoul.port.engine.api.component.Transform.class);
  }

  @Test
  void translateAlongXKeepsX() {
    CoreEngine engine = CoreEngine.builder().build();
    UnityRuntime.install(engine);
    GameObject go = new GameObject("p");
    go.transform().translate(new Vector3(1f, 0f, 0f));
    assertEquals(new Vec3(1f, 0f, 0f), core(engine, go).position());
    assertEquals(new Vector3(1f, 0f, 0f), go.transform().position());
  }

  @Test
  void unityForwardIsCoreMinusZ() {
    CoreEngine engine = CoreEngine.builder().build();
    UnityRuntime.install(engine);
    GameObject go = new GameObject("p");
    go.transform().translate(new Vector3(0f, 2f, 3f));
    assertEquals(new Vec3(0f, 2f, -3f), core(engine, go).position());
    assertEquals(new Vector3(0f, 2f, 3f), go.transform().position());
  }

  @Test
  void translateUsesOwnRotation() {
    CoreEngine engine = CoreEngine.builder().build();
    UnityRuntime.install(engine);
    GameObject go = new GameObject("p");
    var rotated =
        core(engine, go).withRotation(Quat.fromAxisAngle(Vec3.UNIT_Y, (float) (Math.PI / 2.0)));
    engine.world().set(go.entity(), rotated);
    go.transform().translate(new Vector3(1f, 0f, 0f));
    // Core: +X um +Y um 90° gedreht ergibt -Z
    assertTrue(core(engine, go).position().approxEquals(new Vec3(0f, 0f, -1f)));
  }

  @Test
  void getAxisReturnsRawScriptedValue() {
    CoreEngine engine =
        CoreEngine.builder()
            .subsystem(new ScriptedInput(new InputScript().at(0).axis("Horizontal", 0.25f)))
            .build();
    UnityRuntime.install(engine);
    List<Float> values = new ArrayList<>();
    engine.register(Phase.UPDATE, ctx -> values.add(Input.getAxis("Horizontal")));
    engine.step(2);
    assertEquals(List.of(0.25f, 0.25f), values);
  }

  @Test
  void vectorOperatorsFollowUnity() {
    Vector3 a = new Vector3(1f, 2f, 3f);
    assertEquals(new Vector3(2f, 4f, 6f), Vector3.mul(a, 2f));
    assertEquals(new Vector3(2f, 4f, 6f), Vector3.add(a, a));
    assertEquals(new Vector3(0f, 0f, 0f), Vector3.sub(a, a));
    assertTrue(Vector3.equals(a, new Vector3(1f, 2f, 3.000001f)));
    assertTrue(!Vector3.equals(a, new Vector3(1f, 2f, 3.001f)));
  }
}
