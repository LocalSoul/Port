package io.github.localsoul.adapter.unity;

import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.World;

/**
 * {@code UnityEngine.GameObject}: eine Entity der Engine mit Core-Transform und {@link ScriptHost}.
 * Erzeugen ist sofort wirksam; Komponenten der Engine kommen während eines Phasenschritts erst am
 * Phasenende hinzu (ADR-016). Skripte hält der Adapter selbst, sie sind sofort benutzbar.
 */
public class GameObject extends UnityObject {

  private final Entity entity;
  private final String name;
  private final Transform transform;
  private final ScriptHost host;

  /**
   * {@code new GameObject(name)} in der aktiven Engine ({@link UnityRuntime#install}).
   *
   * @param name Name des Objekts
   */
  public GameObject(String name) {
    World world = UnityRuntime.world();
    this.entity = world.create();
    this.name = name;
    this.transform = new Transform(this);
    this.host = new ScriptHost(this);
    world.add(entity, io.github.localsoul.port.engine.api.component.Transform.IDENTITY);
    world.add(entity, host);
    transform.attach(this);
  }

  /**
   * {@code GameObject.name}.
   *
   * @return Name
   */
  public String name() {
    return name;
  }

  /**
   * {@code GameObject.transform}.
   *
   * @return Transform
   */
  public Transform transform() {
    return transform;
  }

  /**
   * Hängt ein Skript an und ruft sofort {@code Awake} und {@code OnEnable} (wie Unity). Unitys
   * {@code AddComponent<T>()} erzeugt die Instanz selbst; hier übergibt der Aufrufer sie (der
   * generische Weg folgt mit dem Mapping in M3a).
   *
   * @param <T> Skripttyp
   * @param script neue Skriptinstanz
   * @return dieselbe Instanz
   */
  public <T extends MonoBehaviour> T addComponent(T script) {
    script.attach(this);
    host.add(script);
    script.awake();
    script.onEnable();
    return script;
  }

  Entity entity() {
    return entity;
  }
}
