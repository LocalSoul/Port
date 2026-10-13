using Xunit;

namespace PortFrontendUnity.Tests;

public class LiteralsTests
{
    [Theory]
    [InlineData(5f, "5.0f32")]
    [InlineData(0f, "0.0f32")]
    [InlineData(-0.5f, "-0.5f32")]
    [InlineData(0.1f, "0.1f32")]
    [InlineData(1e-5f, "1e-05f32")]
    [InlineData(3.4028235e38f, "3.4028235e38f32")]
    public void FormatsFloatsWithPointOrExponent(float value, string expected)
    {
        Assert.Equal(expected, S.Render(Literals.Float(value)));
    }

    [Fact]
    public void FormatsDoubles()
    {
        Assert.Equal("1.5f64", S.Render(Literals.Double(1.5)));
        Assert.Equal("1e100f64", S.Render(Literals.Double(1e100)));
    }

    [Fact]
    public void NonFiniteValuesBecomeStaticFields()
    {
        Assert.Equal("(sfield System.Single::NaN)", S.Render(Literals.Float(float.NaN)));
        Assert.Equal("(sfield System.Double::NegativeInfinity)", S.Render(Literals.Double(double.NegativeInfinity)));
    }

    [Theory]
    [InlineData("a", "\"a\"")]
    [InlineData("tab\there", "\"tab\\there\"")]
    [InlineData("quote\"back\\slash", "\"quote\\\"back\\\\slash\"")]
    [InlineData("line\r\nend", "\"line\\r\\nend\"")]
    [InlineData("ctl\u0001", "\"ctl\\u0001\"")]
    [InlineData("umlaut äöü", "\"umlaut äöü\"")]
    public void EscapesStrings(string value, string expected)
    {
        Assert.Equal(expected, S.Str(value).Text);
    }

    [Fact]
    public void EscapesLoneSurrogates()
    {
        string lone = "lone " + (char)0xD800;
        Assert.Equal("\"lone \\uD800\"", S.Str(lone).Text);
        string pair = "emoji \uD83D\uDE00";
        Assert.Equal("\"emoji \uD83D\uDE00\"", S.Str(pair).Text);
    }

    [Fact]
    public void TypesLiteralsWithoutAnnotationFromClrValue()
    {
        Assert.Equal("2.0f32", S.Render(Literals.FromClrValue(2f)));
        Assert.Equal("7i32", S.Render(Literals.FromClrValue(7)));
        Assert.Equal("7u64", S.Render(Literals.FromClrValue(7UL)));
        Assert.Equal("(char 65)", S.Render(Literals.FromClrValue('A')));
        Assert.Equal("null", S.Render(Literals.FromClrValue(null)));
        Assert.True(Literals.IsPrimitiveClrValue(1.5));
        Assert.False(Literals.IsPrimitiveClrValue("text"));
    }

    [Theory]
    [InlineData("1", "1.0")]
    [InlineData("1E+10", "1e10")]
    [InlineData("1E-05", "1e-05")]
    [InlineData("2.5", "2.5")]
    public void NormalizesNumberText(string input, string expected)
    {
        Assert.Equal(expected, Literals.Normalize(input));
    }
}
