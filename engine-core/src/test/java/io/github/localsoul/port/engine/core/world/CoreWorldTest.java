package io.github.localsoul.port.engine.core.world;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.engine.api.component.Children;
import io.github.localsoul.port.engine.api.component.Parent;
import io.github.localsoul.port.engine.api.component.Transform;
import io.github.localsoul.port.engine.api.component.WorldTransform;
import io.github.localsoul.port.engine.api.math.Mat4;
import io.github.localsoul.port.engine.api.math.Vec3;
import io.github.localsoul.port.engine.api.world.DuplicateComponentException;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.EntityView;
import io.github.localsoul.port.engine.api.world.HierarchyException;
import io.github.localsoul.port.engine.api.world.MissingComponentException;
import io.github.localsoul.port.engine.api.world.ReservedComponentException;
import io.github.localsoul.port.engine.api.world.StaleEntityException;
import io.github.localsoul.port.engine.api.world.UnregisteredComponentException;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Welt außerhalb eines Frames: alle Änderungen wirken sofort. */
class CoreWorldTest {

  /** Eigene Komponente, wie sie ein Adapter registrieren würde. */
  record Health(int value) {}

  /** Adapter-Typ als veränderliche Klasse: der Core speichert ihn nur. */
  static final class ScriptHost {
    final List<String> scripts;

    ScriptHost(List<String> scripts) {
      this.scripts = scripts;
    }

    @Override
    public String toString() {
      return "ScriptHost" + scripts;
    }
  }

  @Test
  void setReplacesAPendingAddDuringAPhase() {
    CoreWorld world = new CoreWorld();
    world.registerComponent(Health.class);
    world.beginPhase();
    Entity entity = world.create();
    world.add(entity, new Health(1));
    world.set(entity, new Health(2));
    assertFalse(world.has(entity, Health.class));
    world.endPhase();
    assertEquals(new Health(2), world.get(entity, Health.class));
    world.beginPhase();
    world.remove(entity, Health.class);
    Entity other = world.create();
    assertThrows(MissingComponentException.class, () -> world.set(other, new Health(3)));
    world.endPhase();
  }

  @Test
  void registersCustomComponentAttachesAndQueries() {
    CoreWorld world = new CoreWorld();
    world.registerComponent(Health.class);
    world.registerComponent(Health.class);
    Entity e = world.create();
    world.add(e, new Health(10));
    assertTrue(world.has(e, Health.class));
    assertEquals(new Health(10), world.get(e, Health.class));
    world.set(e, new Health(7));
    assertEquals(7, world.get(e, Health.class).value());
    assertEquals(1, world.store(Health.class).size());
    world.remove(e, Health.class);
    assertFalse(world.has(e, Health.class));
  }

  @Test
  void storesAdapterDefinedClassesWithoutKnowingThem() {
    CoreWorld world = new CoreWorld();
    world.registerComponent(ScriptHost.class);
    Entity e = world.create();
    ScriptHost host = new ScriptHost(List.of("Mover", "Mover"));
    world.add(e, host);
    assertTrue(world.get(e, ScriptHost.class) == host);
  }

  @Test
  void rejectsUnregisteredDuplicateAndMissingComponents() {
    CoreWorld world = new CoreWorld();
    Entity e = world.create();
    assertThrows(UnregisteredComponentException.class, () -> world.add(e, new Health(1)));
    assertThrows(UnregisteredComponentException.class, () -> world.store(Health.class));
    world.registerComponent(Health.class);
    world.add(e, new Health(1));
    assertThrows(DuplicateComponentException.class, () -> world.add(e, new Health(2)));
    Entity other = world.create();
    assertThrows(MissingComponentException.class, () -> world.get(other, Health.class));
    assertThrows(MissingComponentException.class, () -> world.set(other, new Health(3)));
    assertThrows(MissingComponentException.class, () -> world.remove(other, Health.class));
  }

  @Test
  void staleGenerationThrowsClearException() {
    CoreWorld world = new CoreWorld();
    Entity old = world.create();
    world.destroy(old);
    Entity reused = world.create();
    assertEquals(old.index(), reused.index());
    assertEquals(old.generation() + 1, reused.generation());
    assertFalse(world.isAlive(old));
    StaleEntityException e =
        assertThrows(StaleEntityException.class, () -> world.get(old, Transform.class));
    assertTrue(e.getMessage().contains("veraltet"), e.getMessage());
    assertTrue(e.getMessage().contains("0:0"), e.getMessage());
  }

  @Test
  void unknownAndDestroyedEntitiesAreReported() {
    CoreWorld world = new CoreWorld();
    assertFalse(world.isAlive(Entity.of(5, 0)));
    StaleEntityException unknown =
        assertThrows(StaleEntityException.class, () -> world.destroy(Entity.of(5, 0)));
    assertTrue(unknown.getMessage().contains("existiert nicht"));
    Entity e = world.create();
    world.create();
    world.destroy(e);
    StaleEntityException destroyed =
        assertThrows(StaleEntityException.class, () -> world.destroy(e));
    assertTrue(destroyed.getMessage().contains("zerstört"), destroyed.getMessage());
  }

  @Test
  void reusesFreeIndicesInFifoOrder() {
    CoreWorld world = new CoreWorld();
    Entity a = world.create();
    Entity b = world.create();
    world.create();
    world.destroy(b);
    world.destroy(a);
    assertEquals(b.index(), world.create().index());
    assertEquals(a.index(), world.create().index());
    assertEquals(3, world.create().index());
    assertEquals(List.of(0, 1, 2, 3), indices(world.entities()));
  }

  private static List<Integer> indices(List<Entity> entities) {
    return entities.stream().map(Entity::index).toList();
  }

  @Test
  void coreManagedComponentsCannotBeWrittenDirectly() {
    CoreWorld world = new CoreWorld();
    Entity a = world.create();
    Entity b = world.create();
    assertThrows(ReservedComponentException.class, () -> world.add(a, new Parent(b)));
    assertThrows(ReservedComponentException.class, () -> world.add(a, new Children(List.of(b))));
    assertThrows(
        ReservedComponentException.class, () -> world.add(a, new WorldTransform(Mat4.identity())));
    world.setParent(a, b);
    assertThrows(ReservedComponentException.class, () -> world.remove(a, Parent.class));
  }

  @Test
  void maintainsParentAndOrderedChildren() {
    CoreWorld world = new CoreWorld();
    Entity parent = world.create();
    Entity c1 = world.create();
    Entity c2 = world.create();
    Entity c3 = world.create();
    world.setParent(c2, parent);
    world.setParent(c1, parent);
    world.setParent(c3, parent);
    assertEquals(List.of(c2, c1, c3), world.childrenOf(parent));
    assertEquals(parent, world.parentOf(c1));
    world.setParent(c1, null);
    assertNull(world.parentOf(c1));
    assertEquals(List.of(c2, c3), world.childrenOf(parent));
    world.setParent(c2, c3);
    assertEquals(List.of(c3), world.childrenOf(parent));
    assertEquals(List.of(c2), world.childrenOf(c3));
  }

  @Test
  void rejectsHierarchyCycles() {
    CoreWorld world = new CoreWorld();
    Entity a = world.create();
    Entity b = world.create();
    Entity c = world.create();
    world.setParent(b, a);
    world.setParent(c, b);
    assertThrows(HierarchyException.class, () -> world.setParent(a, c));
    assertThrows(HierarchyException.class, () -> world.setParent(a, a));
  }

  @Test
  void destroyingParentDestroysChildrenRecursively() {
    CoreWorld world = new CoreWorld();
    Entity root = world.create();
    Entity parent = world.create();
    Entity child = world.create();
    Entity grandchild = world.create();
    Entity sibling = world.create();
    world.setParent(parent, root);
    world.setParent(child, parent);
    world.setParent(grandchild, child);
    world.setParent(sibling, root);
    world.destroy(parent);
    assertFalse(world.isAlive(parent));
    assertFalse(world.isAlive(child));
    assertFalse(world.isAlive(grandchild));
    assertTrue(world.isAlive(sibling));
    assertEquals(List.of(sibling), world.childrenOf(root));
  }

  @Test
  void viewListsComponentsSortedByClassName() {
    CoreWorld world = new CoreWorld();
    world.registerComponent(Health.class);
    Entity e = world.create();
    world.add(e, Transform.IDENTITY);
    world.add(e, new Health(3));
    EntityView view = world.view(e);
    assertEquals(e, view.entity());
    assertEquals(List.of(Transform.IDENTITY, new Health(3)), view.components());
  }

  @Test
  void dumpHasDocumentedFormat() {
    CoreWorld world = new CoreWorld();
    world.registerComponent(Health.class);
    Entity a = world.create();
    Entity b = world.create();
    world.add(a, Transform.at(new Vec3(1f, 2f, 3f)));
    world.add(b, new Health(5));
    world.setParent(b, a);
    String expected =
        "world entities=2\n"
            + "entity 0:0\n"
            + "  Children Children[children=[1:0]]\n"
            + "  Transform Transform[position=Vec3[x=1.0, y=2.0, z=3.0],"
            + " rotation=Quat[x=0.0, y=0.0, z=0.0, w=1.0], scale=Vec3[x=1.0, y=1.0, z=1.0]]\n"
            + "entity 1:0\n"
            + "  Parent Parent[parent=0:0]\n"
            + "  Health Health[value=5]\n";
    assertEquals(expected, world.dump());
  }

  @Test
  void dumpIsByteIdenticalForSameInput() {
    assertEquals(buildScene().dump(), buildScene().dump());
  }

  private static CoreWorld buildScene() {
    CoreWorld world = new CoreWorld();
    world.registerComponent(Health.class);
    Entity previous = null;
    for (int i = 0; i < 50; i++) {
      Entity e = world.create();
      world.add(e, Transform.at(new Vec3(i * 0.1f, i, -i)));
      if (i % 3 == 0) {
        world.add(e, new Health(i));
      }
      if (previous != null && i % 2 == 0) {
        world.setParent(e, previous);
      }
      previous = e;
    }
    world.destroy(world.entities().get(10));
    return world;
  }
}
