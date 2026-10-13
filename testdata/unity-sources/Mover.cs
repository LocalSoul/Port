using UnityEngine;

// Beispiel aus docs/ir-spec.md Abschnitt 10.
public class Mover : MonoBehaviour
{
    public float speed = 5.0f;

    private void Update()
    {
        float h = Input.GetAxis("Horizontal");
        transform.Translate(new Vector3(h, 0f, 0f) * speed * Time.deltaTime);
    }
}
