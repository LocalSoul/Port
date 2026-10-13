// Grenzfälle aus dem StarCollector-Probelauf (M2.6):
// - konstant gefaltete Teilausdrücke tragen keine Typannotation am Literal,
// - ein nicht übersetzbarer Feld-Initialisierer (Lambda) wird zu (raw cs ...) als :init.
public class Edges
{
    private System.Func<int> producer = () => 1;

    public float Angle(int i, int count)
    {
        return System.MathF.PI / 2f + i * System.MathF.PI / count;
    }

    public int Produce()
    {
        return producer();
    }
}
