using System;
using System.Collections.Generic;

// BCL-Semantik (M3a, ADR-019): Dictionary-Reihenfolge nach Remove/Add, foreach über KeyValuePair,
// try/catch/finally, throw, Null-Zugriff als NullReferenceException, Zahlformat in Verkettungen.
public class Ledger
{
    private Dictionary<string, int> entries = new Dictionary<string, int>();

    public string Order()
    {
        entries.Clear();
        entries.Add("a", 1);
        entries.Add("b", 2);
        entries.Add("c", 3);
        entries.Remove("b");
        entries.Add("d", 4);
        string text = "";
        foreach (KeyValuePair<string, int> pair in entries)
        {
            text = text + pair.Key + "=" + pair.Value + ";";
        }
        return text;
    }

    public int Lookup(string key)
    {
        try
        {
            return entries[key];
        }
        catch (KeyNotFoundException)
        {
            return -1;
        }
    }

    public string Guard(Ledger other)
    {
        string log = "try;";
        try
        {
            other.entries.Clear();
            log = log + "ok;";
        }
        catch (NullReferenceException e)
        {
            log = log + "nre:" + e.Message + ";";
        }
        finally
        {
            log = log + "finally";
        }
        return log;
    }

    public string Check(int amount)
    {
        try
        {
            if (amount < 0)
            {
                throw new InvalidOperationException("negativ");
            }
            return "ok";
        }
        catch (Exception e)
        {
            return e.Message;
        }
    }

    public string Format(float value)
    {
        return "v=" + value;
    }
}
