package gen.fixtures;

import io.github.localsoul.adapter.unity.Input;
import io.github.localsoul.adapter.unity.MonoBehaviour;
import io.github.localsoul.adapter.unity.Time;
import io.github.localsoul.adapter.unity.Vector3;

/** Generiert aus dem IR-Typ {@code Mover}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class MoverBase extends MonoBehaviour {
  public float speed = 5.0f;

  @Override
  protected void update() {
    float h = Input.getAxis("Horizontal");
    this.transform()
        .translate(
            Vector3.mul(Vector3.mul(new Vector3(h, 0.0f, 0.0f), this.speed), Time.deltaTime()));
  }
}
