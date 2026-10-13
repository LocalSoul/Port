package io.github.localsoul.adapter.unity;

import io.github.localsoul.port.engine.api.math.Vec3;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.World;

/**
 * {@code UnityEngine.Transform} über der Core-Komponente {@code Transform}. Alle Werte nach außen
 * in Unity-Koordinaten, intern im Core-System (ADR-007).
 */
public class Transform extends Component {

  private final GameObject owner;

  Transform(GameObject owner) {
    this.owner = owner;
  }

  private Entity entity() {
    return owner.entity();
  }

  private static World world() {
    return UnityRuntime.world();
  }

  private io.github.localsoul.port.engine.api.component.Transform core() {
    return world().get(entity(), io.github.localsoul.port.engine.api.component.Transform.class);
  }

  /**
   * {@code Transform.position}: Weltposition, sofort aus der Elternkette berechnet.
   *
   * @return Position in Unity-Koordinaten
   */
  public Vector3 position() {
    return Vector3.fromCore(world().worldMatrix(entity()).translation());
  }

  /**
   * {@code Transform.Translate(Vector3)} mit {@code Space.Self}: verschiebt um {@code translation}
   * entlang der eigenen Achsen. Korrekt für Objekte ohne rotiertes oder skaliertes Elternobjekt
   * (M3); allgemeiner Fall folgt bei Bedarf.
   *
   * @param translation Verschiebung in Unity-Koordinaten
   */
  public void translate(Vector3 translation) {
    io.github.localsoul.port.engine.api.component.Transform current = core();
    Vec3 delta = current.rotation().rotate(translation.toCore());
    world().set(entity(), current.withPosition(current.position().add(delta)));
  }
}
