using UnityEngine;

// Serialisiertes privates Feld, Property mit explizitem Feld, statisches Feld.
public class Holder : MonoBehaviour
{
    public static int Instances;

    [SerializeField]
    private float health = 100f;

    private int level;

    public int Level
    {
        get { return level; }
        set { level = Mathf.Clamp(value, 0, 10); }
    }

    public float Health
    {
        get { return health; }
    }

    private void Awake()
    {
        Instances++;
    }
}
