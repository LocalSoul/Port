package io.github.localsoul.port.engine.api.component;

import io.github.localsoul.port.engine.api.subsystem.ComponentType;
import java.util.Set;

/** Namen und Versionen der Komponentenschemas für {@code SubsystemInfo}. */
public final class Schemas {

  /** {@link Transform}. */
  public static final ComponentType TRANSFORM =
      new ComponentType("Transform", Transform.SCHEMA_VERSION);

  /** {@link WorldTransform}, Besitzer: Core. */
  public static final ComponentType WORLD_TRANSFORM =
      new ComponentType("WorldTransform", WorldTransform.SCHEMA_VERSION);

  /** {@link Parent}, Besitzer: Core. */
  public static final ComponentType PARENT = new ComponentType("Parent", Parent.SCHEMA_VERSION);

  /** {@link Children}, Besitzer: Core. */
  public static final ComponentType CHILDREN =
      new ComponentType("Children", Children.SCHEMA_VERSION);

  /** {@link Renderable}. */
  public static final ComponentType RENDERABLE =
      new ComponentType("Renderable", Renderable.SCHEMA_VERSION);

  /** {@link InputState}. */
  public static final ComponentType INPUT_STATE =
      new ComponentType("InputState", InputState.SCHEMA_VERSION);

  /** Vom Core geschriebene Komponenten; kein Subsystem darf sie schreiben. */
  /** {@link Texture}. */
  public static final ComponentType TEXTURE = new ComponentType("Texture", Texture.SCHEMA_VERSION);

  /** {@link Sprite}. */
  public static final ComponentType SPRITE = new ComponentType("Sprite", Sprite.SCHEMA_VERSION);

  /** {@link Camera2D}. */
  public static final ComponentType CAMERA_2D =
      new ComponentType("Camera2D", Camera2D.SCHEMA_VERSION);

  /** {@link Viewport}. */
  public static final ComponentType VIEWPORT =
      new ComponentType("Viewport", Viewport.SCHEMA_VERSION);

  /** {@link UiCanvas}. */
  public static final ComponentType UI_CANVAS =
      new ComponentType("UiCanvas", UiCanvas.SCHEMA_VERSION);

  public static final Set<ComponentType> CORE_OWNED = Set.of(WORLD_TRANSFORM, PARENT, CHILDREN);

  private Schemas() {}
}
