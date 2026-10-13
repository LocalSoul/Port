package io.github.localsoul.port.engine.core.world;

import io.github.localsoul.port.engine.api.component.Children;
import io.github.localsoul.port.engine.api.component.InputState;
import io.github.localsoul.port.engine.api.component.Parent;
import io.github.localsoul.port.engine.api.component.Renderable;
import io.github.localsoul.port.engine.api.component.Transform;
import io.github.localsoul.port.engine.api.component.WorldTransform;
import io.github.localsoul.port.engine.api.math.Mat4;
import io.github.localsoul.port.engine.api.world.ComponentStore;
import io.github.localsoul.port.engine.api.world.DuplicateComponentException;
import io.github.localsoul.port.engine.api.world.Entity;
import io.github.localsoul.port.engine.api.world.EntityView;
import io.github.localsoul.port.engine.api.world.HierarchyException;
import io.github.localsoul.port.engine.api.world.MissingComponentException;
import io.github.localsoul.port.engine.api.world.ReservedComponentException;
import io.github.localsoul.port.engine.api.world.UnregisteredComponentException;
import io.github.localsoul.port.engine.api.world.World;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Implementierung von {@link World} nach ADR-016.
 *
 * <p>Zwischen {@link #beginPhase()} und {@link #endPhase()} werden Strukturänderungen im
 * Command-Buffer gesammelt. Ihre Vorbedingungen werden schon beim Aufruf gegen den erwarteten
 * Zustand geprüft (aktueller Zustand plus anstehende Befehle), damit Fehler beim Verursacher
 * auftreten. {@link #endPhase()} führt die Befehle in Aufrufreihenfolge aus; Befehle für bis dahin
 * zerstörte Entities entfallen.
 */
public final class CoreWorld implements World {

  private static final Set<Class<?>> RESERVED =
      Set.of(Parent.class, Children.class, WorldTransform.class);

  private final EntityTable entityTable = new EntityTable();
  // Nur für Lookups; geordnete Ausgaben sortieren explizit nach Klassennamen.
  private final Map<Class<?>, SparseSetStore<?>> stores = new HashMap<>();
  private final CommandBuffer buffer = new CommandBuffer();
  private boolean deferring;

  /** Erzeugt eine leere Welt mit registrierten Core-Schemas. */
  public CoreWorld() {
    registerComponent(Transform.class);
    registerComponent(WorldTransform.class);
    registerComponent(Parent.class);
    registerComponent(Children.class);
    registerComponent(Renderable.class);
    registerComponent(InputState.class);
  }

  // --- Phasensteuerung (nur Core) ---

  /** Ab jetzt werden Strukturänderungen gesammelt. */
  public void beginPhase() {
    deferring = true;
  }

  /**
   * Beendet die Phase und führt gesammelte Strukturänderungen aus.
   *
   * @throws HierarchyException wenn ein gesammeltes {@code setParent} zusammen mit anderen Befehlen
   *     derselben Phase einen Zyklus ergäbe; alle übrigen Befehle sind dann ausgeführt
   */
  public void endPhase() {
    deferring = false;
    HierarchyException firstError = null;
    for (Command command : buffer.drain()) {
      try {
        apply(command);
      } catch (HierarchyException e) {
        if (firstError == null) {
          firstError = e;
        }
      }
    }
    if (firstError != null) {
      throw firstError;
    }
  }

  /**
   * Liefert, ob gerade Strukturänderungen gesammelt werden.
   *
   * @return {@code true} innerhalb einer Phase
   */
  public boolean isDeferring() {
    return deferring;
  }

  /**
   * Schreibt eine vom Core verwaltete Komponente sofort (z. B. {@code WorldTransform}).
   *
   * @param <T> Komponententyp
   * @param entity lebende Entity
   * @param component Komponente
   */
  public <T> void putInternal(Entity entity, T component) {
    entityTable.check(entity);
    storeFor(component.getClass()).putObject(entity, component);
  }

  /**
   * Entfernt eine vom Core verwaltete Komponente sofort, falls vorhanden.
   *
   * @param entity Entity
   * @param type Typ
   */
  public void removeInternal(Entity entity, Class<?> type) {
    storeFor(type).remove(entity);
  }

  // --- Entities ---

  @Override
  public Entity create() {
    return entityTable.create();
  }

  @Override
  public void destroy(Entity entity) {
    entityTable.check(entity);
    if (deferring) {
      buffer.add(new Command.Destroy(entity));
    } else {
      destroyNow(entity);
    }
  }

  @Override
  public boolean isAlive(Entity entity) {
    return entityTable.isAlive(entity);
  }

  @Override
  public List<Entity> entities() {
    return List.copyOf(entityTable.aliveEntities());
  }

  // --- Komponenten ---

  @Override
  public <T> void registerComponent(Class<T> type) {
    if (!stores.containsKey(type)) {
      stores.put(type, new SparseSetStore<>(type));
    }
  }

  @Override
  public <T> void add(Entity entity, T component) {
    entityTable.check(entity);
    Class<?> type = component.getClass();
    checkWritable(type);
    if (willHave(entity, type)) {
      throw new DuplicateComponentException(
          "Entity " + entity + " hat bereits " + type.getSimpleName());
    }
    if (deferring) {
      buffer.add(new Command.Add(entity, component));
    } else {
      storeFor(type).putObject(entity, component);
    }
  }

  @Override
  public <T> void set(Entity entity, T component) {
    entityTable.check(entity);
    Class<?> type = component.getClass();
    checkWritable(type);
    SparseSetStore<?> store = storeFor(type);
    if (!store.contains(entity)) {
      throw new MissingComponentException(
          "Entity " + entity + " hat kein " + type.getSimpleName() + " (set ersetzt nur)");
    }
    store.putObject(entity, component);
  }

  @Override
  public <T> T get(Entity entity, Class<T> type) {
    entityTable.check(entity);
    T component = storeFor(type).getOrNull(entity);
    if (component == null) {
      throw new MissingComponentException("Entity " + entity + " hat kein " + type.getSimpleName());
    }
    return component;
  }

  @Override
  public <T> boolean has(Entity entity, Class<T> type) {
    entityTable.check(entity);
    return storeFor(type).contains(entity);
  }

  @Override
  public <T> void remove(Entity entity, Class<T> type) {
    entityTable.check(entity);
    checkWritable(type);
    if (!willHave(entity, type)) {
      throw new MissingComponentException(
          "Entity " + entity + " hat kein " + type.getSimpleName() + " zum Entfernen");
    }
    if (deferring) {
      buffer.add(new Command.Remove(entity, type));
    } else {
      storeFor(type).remove(entity);
    }
  }

  @Override
  public <T> ComponentStore<T> store(Class<T> type) {
    return storeFor(type);
  }

  // --- Hierarchie ---

  @Override
  public void setParent(Entity child, Entity parent) {
    entityTable.check(child);
    if (parent != null) {
      entityTable.check(parent);
      checkNoCycle(child, parent);
    }
    if (deferring) {
      buffer.add(new Command.SetParent(child, parent));
    } else {
      setParentNow(child, parent);
    }
  }

  @Override
  public Entity parentOf(Entity entity) {
    entityTable.check(entity);
    Parent parent = storeFor(Parent.class).getOrNull(entity);
    return parent == null ? null : parent.parent();
  }

  @Override
  public List<Entity> childrenOf(Entity entity) {
    entityTable.check(entity);
    Children children = storeFor(Children.class).getOrNull(entity);
    return children == null ? List.of() : children.children();
  }

  @Override
  public Mat4 worldMatrix(Entity entity) {
    entityTable.check(entity);
    List<Entity> chain = new ArrayList<>();
    Entity current = entity;
    while (current != null) {
      chain.add(current);
      current = parentOf(current);
    }
    Mat4 result = Mat4.identity();
    for (int i = chain.size() - 1; i >= 0; i--) {
      result = result.multiply(localMatrix(chain.get(i)));
    }
    return result;
  }

  private Mat4 localMatrix(Entity entity) {
    Transform transform = storeFor(Transform.class).getOrNull(entity);
    if (transform == null) {
      return Mat4.identity();
    }
    return Mat4.trs(transform.position(), transform.rotation(), transform.scale());
  }

  // --- Debugging ---

  @Override
  public EntityView view(Entity entity) {
    entityTable.check(entity);
    List<Object> components = new ArrayList<>();
    for (SparseSetStore<?> store : storesByName().values()) {
      Object component = store.getOrNull(entity);
      if (component != null) {
        components.add(component);
      }
    }
    return new EntityView(entity, components);
  }

  @Override
  public String dump() {
    List<Entity> alive = entityTable.aliveEntities();
    StringBuilder out = new StringBuilder();
    out.append("world entities=").append(alive.size()).append('\n');
    for (Entity entity : alive) {
      out.append("entity ").append(entity).append('\n');
      for (Object component : view(entity).components()) {
        out.append("  ").append(component.getClass().getSimpleName()).append(' ');
        out.append(component).append('\n');
      }
    }
    return out.toString();
  }

  // --- Ausführung ---

  private void apply(Command command) {
    switch (command) {
      case Command.Destroy destroy -> {
        if (entityTable.isAlive(destroy.entity())) {
          destroyNow(destroy.entity());
        }
      }
      case Command.Add add -> {
        if (entityTable.isAlive(add.entity())) {
          storeFor(add.component().getClass()).putObject(add.entity(), add.component());
        }
      }
      case Command.Remove remove -> {
        if (entityTable.isAlive(remove.entity())) {
          storeFor(remove.type()).remove(remove.entity());
        }
      }
      case Command.SetParent setParent -> applySetParent(setParent);
    }
  }

  private void applySetParent(Command.SetParent command) {
    Entity child = command.child();
    Entity parent = command.parent();
    if (!entityTable.isAlive(child)) {
      return;
    }
    if (parent != null && !entityTable.isAlive(parent)) {
      return;
    }
    if (parent != null) {
      checkNoCycle(child, parent);
    }
    setParentNow(child, parent);
  }

  private void destroyNow(Entity root) {
    List<Entity> subtree = new ArrayList<>();
    collectPostOrder(root, subtree);
    setParentNow(root, null);
    for (Entity entity : subtree) {
      // Reihenfolge egal: nur Entfernen, keine Ausgabe.
      for (SparseSetStore<?> store : stores.values()) {
        store.remove(entity);
      }
      entityTable.destroy(entity);
    }
  }

  /** Kinder vor dem Elternobjekt, Kinder in Anhängereihenfolge. */
  private void collectPostOrder(Entity entity, List<Entity> into) {
    for (Entity child : childrenOf(entity)) {
      collectPostOrder(child, into);
    }
    into.add(entity);
  }

  private void setParentNow(Entity child, Entity parent) {
    Entity oldParent = parentOf(child);
    if (oldParent != null) {
      detachChild(oldParent, child);
    }
    if (parent == null) {
      storeFor(Parent.class).remove(child);
      return;
    }
    storeFor(Parent.class).putObject(child, new Parent(parent));
    List<Entity> siblings = new ArrayList<>(childrenOf(parent));
    siblings.add(child);
    storeFor(Children.class).putObject(parent, new Children(siblings));
  }

  private void detachChild(Entity parent, Entity child) {
    List<Entity> siblings = new ArrayList<>(childrenOf(parent));
    siblings.remove(child);
    if (siblings.isEmpty()) {
      storeFor(Children.class).remove(parent);
    } else {
      storeFor(Children.class).putObject(parent, new Children(siblings));
    }
  }

  private void checkNoCycle(Entity child, Entity parent) {
    Entity current = parent;
    while (current != null) {
      if (current.equals(child)) {
        throw new HierarchyException(
            "Entity " + child + " kann nicht Kind von " + parent + " werden (Zyklus)");
      }
      current = parentOf(current);
    }
  }

  /** Erwarteter Besitz nach Ausführung aller anstehenden Befehle. */
  private boolean willHave(Entity entity, Class<?> type) {
    boolean result = storeFor(type).contains(entity);
    for (Command command : buffer.pending()) {
      if (command instanceof Command.Add add
          && add.entity().equals(entity)
          && add.component().getClass() == type) {
        result = true;
      } else if (command instanceof Command.Remove remove
          && remove.entity().equals(entity)
          && remove.type() == type) {
        result = false;
      }
    }
    return result;
  }

  private void checkWritable(Class<?> type) {
    if (RESERVED.contains(type)) {
      throw new ReservedComponentException(
          type.getSimpleName() + " wird vom Core verwaltet (setParent bzw. LATE-Berechnung)");
    }
  }

  private <T> SparseSetStore<T> storeFor(Class<T> type) {
    SparseSetStore<?> store = stores.get(type);
    if (store == null) {
      throw new UnregisteredComponentException(
          "Komponententyp " + type.getName() + " ist nicht registriert");
    }
    @SuppressWarnings("unchecked") // Schlüssel und Speichertyp werden nur gemeinsam eingetragen
    SparseSetStore<T> typed = (SparseSetStore<T>) store;
    return typed;
  }

  private TreeMap<String, SparseSetStore<?>> storesByName() {
    TreeMap<String, SparseSetStore<?>> sorted = new TreeMap<>();
    for (Map.Entry<Class<?>, SparseSetStore<?>> entry : stores.entrySet()) {
      sorted.put(entry.getKey().getName(), entry.getValue());
    }
    return sorted;
  }
}
