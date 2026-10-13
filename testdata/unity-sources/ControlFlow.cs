// Kontrollfluss ohne Unity-Typen: if, while, for, foreach über Arrays, switch über int und string.
public class ControlFlow
{
    public int Sum(int[] values)
    {
        int sum = 0;
        foreach (int v in values)
        {
            if (v < 0)
            {
                continue;
            }
            sum += v;
        }
        return sum;
    }

    public int CountDown(int start)
    {
        int steps = 0;
        while (start > 0)
        {
            start--;
            steps++;
            if (steps > 100)
            {
                break;
            }
        }
        return steps;
    }

    public float Average(float[] values)
    {
        if (values.Length == 0)
        {
            return 0f;
        }
        float total = 0f;
        for (int i = 0; i < values.Length; i++)
        {
            total += values[i];
        }
        return total / values.Length;
    }

    public string Describe(int code)
    {
        switch (code)
        {
            case 0:
                return "null";
            case 1:
            case 2:
                return "klein";
            default:
                return "groß";
        }
    }

    public int Score(string grade)
    {
        switch (grade)
        {
            case "A":
                return 3;
            case "B":
                return 2;
            default:
                return 0;
        }
    }
}
