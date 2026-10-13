package io.github.localsoul.adapter.unity;

import io.github.localsoul.port.engine.api.Engine;
import io.github.localsoul.port.engine.api.schedule.FrameContext;
import io.github.localsoul.port.engine.api.schedule.Phase;
import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.World;
import java.util.ArrayList;
import java.util.List;

/**
 * Verbindet den Adapter mit einer Engine und treibt den Unity-Lifecycle.
 *
 * <p>Die Unity-API ist statisch ({@code Time.deltaTime}, {@code Input.GetAxis}). Deshalb hält diese
 * Klasse die aktive Engine und den {@link FrameContext} des laufenden Phasenschritts als statischen
 * Zustand. Es gibt genau eine aktive Engine je JVM; {@link #install(Engine)} ersetzt sie.
 *
 * <p>Lifecycle (Unity-Dokumentation „Execution Order of Event Functions"):
 *
 * <ul>
 *   <li>{@code Awake} und {@code OnEnable} sofort bei {@link GameObject#addComponent} (wie Unity).
 *   <li>{@code Start} in {@code EARLY} vor dem ersten {@code Update} des Skripts, einmalig.
 *   <li>{@code FixedUpdate} in jedem {@code FIXED}-Schritt, {@code Update} in {@code UPDATE},
 *       {@code LateUpdate} in {@code LATE}.
 *   <li>Reihenfolge: Entities aufsteigend, je Entity Skripte in Anhängereihenfolge. Unity legt sie
 *       nicht fest; Abweichung bewusst deterministisch.
 *   <li>Skripte, die während eines Phasenschritts hinzukommen, laufen ab dem nächsten Schritt
 *       (Kopie der Liste je Schritt).
 *   <li>Der Kontext gilt ab dem jeweiligen Adapter-Callback bis zum Ende des {@code LATE}-Callbacks
 *       des Adapters; danach liefern {@code Time} und {@code Input} die Werte für „außerhalb eines
 *       Frames".
 * </ul>
 */
public final class UnityRuntime {

  private static Engine engine;
  private static FrameContext current;

  private UnityRuntime() {}

  /**
   * Bindet den Adapter an eine Engine: registriert {@link ScriptHost} und die Lifecycle-Callbacks.
   *
   * @param target Engine
   */
  public static void install(Engine target) {
    engine = target;
    current = null;
    target.world().registerComponent(ScriptHost.class);
    target.register(Phase.EARLY, UnityRuntime::early);
    target.register(Phase.FIXED, UnityRuntime::fixed);
    target.register(Phase.UPDATE, UnityRuntime::update);
    target.register(Phase.LATE, UnityRuntime::late);
  }

  /**
   * Liefert die Welt der aktiven Engine.
   *
   * @return Welt
   * @throws IllegalStateException ohne {@link #install(Engine)}
   */
  static World world() {
    if (engine == null) {
      throw new IllegalStateException("UnityRuntime.install(engine) wurde nicht aufgerufen");
    }
    return engine.world();
  }

  /**
   * Liefert den Kontext des laufenden Phasenschritts.
   *
   * @return Kontext oder {@code null} außerhalb eines Frames
   */
  static FrameContext current() {
    return current;
  }

  private static void early(FrameContext context) {
    current = context;
    for (MonoBehaviour script : scripts()) {
      script.startIfNeeded();
    }
  }

  private static void fixed(FrameContext context) {
    current = context;
    for (MonoBehaviour script : startedScripts()) {
      script.fixedUpdate();
    }
  }

  private static void update(FrameContext context) {
    current = context;
    for (MonoBehaviour script : startedScripts()) {
      script.update();
    }
  }

  /** Letzter Adapter-Schritt des Frames; danach gilt „außerhalb eines Frames" für Time/Input. */
  private static void late(FrameContext context) {
    current = context;
    for (MonoBehaviour script : startedScripts()) {
      script.lateUpdate();
    }
    current = null;
  }

  /** Alle Skripte, Entities aufsteigend, je Entity in Anhängereihenfolge (Kopie). */
  private static List<MonoBehaviour> scripts() {
    List<MonoBehaviour> result = new ArrayList<>();
    ComponentStore<ScriptHost> hosts = world().store(ScriptHost.class);
    for (int i = 0; i < hosts.size(); i++) {
      result.addAll(hosts.componentAt(i).scripts());
    }
    return result;
  }

  private static List<MonoBehaviour> startedScripts() {
    List<MonoBehaviour> result = new ArrayList<>();
    for (MonoBehaviour script : scripts()) {
      if (script.isStarted()) {
        result.add(script);
      }
    }
    return result;
  }
}
