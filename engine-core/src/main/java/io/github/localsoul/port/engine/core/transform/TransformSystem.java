package io.github.localsoul.port.engine.core.transform;

import io.github.localsoul.port.engine.api.component.Transform;
import io.github.localsoul.port.engine.api.component.WorldTransform;
import io.github.localsoul.port.engine.api.math.Mat4;
import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.core.world.CoreWorld;
import java.util.ArrayList;
import java.util.List;

/**
 * Schreibt {@link WorldTransform} für jede Entity mit {@link Transform} (Besitzer: Core). Läuft zu
 * Beginn von {@code LATE}, nach der Spiellogik in {@code UPDATE}. Entities ohne {@code Transform}
 * verlieren ein veraltetes {@code WorldTransform}.
 *
 * <p>Die Berechnung nutzt {@code World.worldMatrix} je Entity (Elternkette). Das ist O(n · Tiefe)
 * und für M1 ausreichend; ein Cache je Frame ist möglich, ohne die API zu ändern.
 */
public final class TransformSystem {

  private TransformSystem() {}

  /**
   * Aktualisiert alle Welttransformationen sofort.
   *
   * @param world Welt
   */
  public static void update(CoreWorld world) {
    removeStale(world);
    ComponentStore<Transform> transforms = world.store(Transform.class);
    for (int i = 0; i < transforms.size(); i++) {
      Entity entity = transforms.entityAt(i);
      Mat4 matrix = world.worldMatrix(entity);
      world.putInternal(entity, new WorldTransform(matrix));
    }
  }

  private static void removeStale(CoreWorld world) {
    ComponentStore<WorldTransform> worldTransforms = world.store(WorldTransform.class);
    List<Entity> stale = new ArrayList<>();
    for (int i = 0; i < worldTransforms.size(); i++) {
      Entity entity = worldTransforms.entityAt(i);
      if (!world.has(entity, Transform.class)) {
        stale.add(entity);
      }
    }
    for (Entity entity : stale) {
      world.removeInternal(entity, WorldTransform.class);
    }
  }
}
