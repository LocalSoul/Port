package io.github.localsoul.adapter.unity;

/**
 * {@code UnityEngine.MonoBehaviour}: Basis der Spielskripte. Die Lifecycle-Methoden sind
 * überschreibbare No-ops; der Generator erzeugt sie in Unterklassen als {@code @Override} (Form
 * {@code message} in {@code mapping.tsv}). Aufgerufen werden sie von {@link UnityRuntime} bzw.
 * {@link GameObject#addComponent}.
 */
public class MonoBehaviour extends Behaviour {

  private boolean started;

  /** Erzeugt das Skript. */
  protected MonoBehaviour() {}

  /** {@code Awake}: direkt beim Anhängen. */
  protected void awake() {}

  /** {@code OnEnable}: direkt nach {@code Awake}. */
  protected void onEnable() {}

  /** {@code Start}: einmal vor dem ersten {@code Update}. */
  protected void start() {}

  /** {@code Update}: einmal pro Frame. */
  protected void update() {}

  /** {@code FixedUpdate}: in jedem festen Zeitschritt. */
  protected void fixedUpdate() {}

  /** {@code LateUpdate}: einmal pro Frame nach allen {@code Update}. */
  protected void lateUpdate() {}

  boolean isStarted() {
    return started;
  }

  void startIfNeeded() {
    if (!started) {
      started = true;
      start();
    }
  }
}
