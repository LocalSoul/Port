using System.Collections.Generic;
using Xunit;

namespace PortFrontendUnity.Tests;

public class NamesAndGlobTests
{
    [Theory]
    [InlineData("speed", "speed")]
    [InlineData("_x1", "_x1")]
    [InlineData("größe", "gr_u00F6_u00DFe")]
    [InlineData("<Count>k__BackingField", "_u003CCount_u003Ek__BackingField")]
    [InlineData("1st", "_u0031st")]
    public void MapsNamesToIrIdentifiers(string name, string expected)
    {
        Assert.Equal(expected, Names.Ident(name));
    }

    [Fact]
    public void StripsVerbatimPrefixFromLocals()
    {
        Assert.Equal("class_", Names.Local("@class_"));
    }

    [Theory]
    [InlineData("Mover", "Mover", true)]
    [InlineData("Mov*", "Mover", true)]
    [InlineData("*er", "Mover", true)]
    [InlineData("M?ver", "Mover", true)]
    [InlineData("Game.*", "Game.Board", true)]
    [InlineData("Game.*", "Mover", false)]
    [InlineData("Mover", "MoverX", false)]
    public void MatchesGlobs(string pattern, string name, bool expected)
    {
        Assert.Equal(expected, Glob.MatchesAny(new List<string> { pattern }, name));
    }

    [Fact]
    public void EmptyPatternListMatchesEverything()
    {
        Assert.True(Glob.MatchesAny(new List<string>(), "Anything"));
    }
}
