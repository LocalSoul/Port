package io.github.localsoul.adapter.unity;

/** {@code UnityEngine.Component}: an ein {@link GameObject} gehängtes Objekt. */
public class Component extends UnityObject {

  private GameObject gameObject;

  /** Erzeugt die Komponente, noch ohne {@link GameObject}. */
  protected Component() {}

  /** Verbindet die Komponente mit ihrem Objekt (beim Anhängen). */
  void attach(GameObject owner) {
    this.gameObject = owner;
  }

  /**
   * {@code Component.gameObject}.
   *
   * @return das Objekt, an dem die Komponente hängt
   * @throws IllegalStateException wenn sie noch nicht angehängt ist
   */
  public GameObject gameObject() {
    if (gameObject == null) {
      throw new IllegalStateException(getClass().getSimpleName() + " hängt an keinem GameObject");
    }
    return gameObject;
  }

  /**
   * {@code Component.transform}.
   *
   * @return die Transform des Objekts
   */
  public Transform transform() {
    return gameObject().transform();
  }
}
