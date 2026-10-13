using UnityEngine;

// Felder, Inkrement, zusammengesetzte Zuweisung, String-Verkettung, Auto-Property.
public class Counter : MonoBehaviour
{
    public string label = "Zähler";

    private int count;

    public int Total { get; set; }

    private void Update()
    {
        count++;
        Total += 2;
        if (Input.GetKey("space"))
        {
            count += 10;
        }
        Debug.Log(label + ": " + count);
    }

    public int Twice(int value)
    {
        return value * 2;
    }
}
