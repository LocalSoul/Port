using System.Collections.Generic;

// Generische Typen (ADR-025): eigener generischer Typ, List<T> mit Konstruktor, Methoden, Count und
// Indexer (get_Item/set_Item), Delegate-Aufruf.
public class Box<T>
{
    private T value;

    public void Put(T item)
    {
        value = item;
    }

    public T Get()
    {
        return value;
    }
}

public class Inventory
{
    private List<int> items = new List<int>();

    private Box<string> label = new Box<string>();

    private System.Func<int> counter;

    public int Total()
    {
        int sum = 0;
        for (int i = 0; i < items.Count; i++)
        {
            sum += items[i];
        }
        return sum;
    }

    public void Fill(int count)
    {
        items.Clear();
        for (int i = 0; i < count; i++)
        {
            items.Add(i);
        }
        items[0] = 42;
        items.RemoveAt(count - 1);
        label.Put("voll");
    }

    public string Name()
    {
        return label.Get();
    }

    public void SetCounter(System.Func<int> source)
    {
        counter = source;
    }

    public int Count()
    {
        return counter();
    }
}
