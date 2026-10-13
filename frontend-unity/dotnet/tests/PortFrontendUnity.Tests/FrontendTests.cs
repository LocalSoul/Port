using System;
using System.IO;
using System.Linq;
using Xunit;

namespace PortFrontendUnity.Tests;

public class FrontendTests
{
    private static (int Exit, string Out, string Error) Run(params string[] args)
    {
        Options? options = Options.Parse(args, out string? parseError);
        if (options == null)
        {
            return (Frontend.InvalidArguments, "", parseError ?? "");
        }
        var error = new StringWriter();
        int exit = Frontend.Run(options, error);
        return (exit, options.OutDir, error.ToString());
    }

    [Fact]
    public void TranslatesAllFixturesToOneFilePerType()
    {
        string outDir = Fixtures.TempDir();
        var result = Run("--assembly", Fixtures.Assembly(), "--out", outDir, "--no-sha256");
        Assert.Equal(0, result.Exit);
        string[] files = Directory.GetFiles(outDir).Select(Path.GetFileName).OrderBy(n => n, StringComparer.Ordinal).ToArray()!;
        Assert.Equal(new[] { "ControlFlow.ir", "Counter.ir", "Edges.ir", "Holder.ir", "Mover.ir", "UnsafeBits.ir" }, files);
        Assert.Contains("6 Typen", result.Error);
    }

    [Fact]
    public void UsesLocalNamesFromEmbeddedPdb()
    {
        string outDir = Fixtures.TempDir();
        Run("--assembly", Fixtures.Assembly(), "--out", outDir, "--classes", "Mover");
        string text = File.ReadAllText(Path.Combine(outDir, "Mover.ir"));
        Assert.Contains("(local h f32 (scall UnityEngine.Input::GetAxis#string \"Horizontal\"))", text);
        Assert.StartsWith("(ir", text);
        Assert.Contains(":assembly", text);
        Assert.Matches(":sha256\\s+\"[0-9a-f]{64}\"", text);
    }

    [Fact]
    public void UnsafeCodeBecomesRawWithManualAndRestIsTranslated()
    {
        string outDir = Fixtures.TempDir();
        var result = Run("--assembly", Fixtures.Assembly(), "--out", outDir, "--classes", "UnsafeBits", "--no-sha256");
        Assert.Equal(0, result.Exit);
        string text = File.ReadAllText(Path.Combine(outDir, "UnsafeBits.ir"));
        Assert.Contains("(raw cs \"fixed (int* p = values)", text);
        Assert.Contains(":manual\n    \"fixed-Block (unsafe, Zeiger)\"", text);
        Assert.Contains("(method Plain i32 ((a i32)) :mods (public) (block (return (+ a 1i32))))", text);
        Assert.Contains("1 raw-Stellen", result.Error);
        Assert.Contains("raw: UnsafeBits::First: fixed-Block (unsafe, Zeiger)", result.Error);
    }

    [Fact]
    public void FoldedConstantsAndUntranslatableInitializers()
    {
        string outDir = Fixtures.TempDir();
        var result = Run("--assembly", Fixtures.Assembly(), "--out", outDir, "--classes", "Edges", "--no-sha256");
        Assert.Equal(0, result.Exit);
        string text = File.ReadAllText(Path.Combine(outDir, "Edges.ir"));
        Assert.Contains("(sfield System.MathF::PI) 2.0f32", text);
        Assert.Contains("(raw cs \"() => 1\")", text);
        Assert.Contains("raw: Edges::producer:", result.Error);
    }

    [Fact]
    public void TwoRunsProduceIdenticalOutput()
    {
        string first = Fixtures.TempDir();
        string second = Fixtures.TempDir();
        Run("--assembly", Fixtures.Assembly(), "--out", first);
        Run("--assembly", Fixtures.Assembly(), "--out", second);
        foreach (string file in Directory.GetFiles(first))
        {
            string name = Path.GetFileName(file);
            Assert.Equal(File.ReadAllText(file), File.ReadAllText(Path.Combine(second, name)));
        }
    }

    [Fact]
    public void MissingAssemblyGivesExitCode3()
    {
        var result = Run("--assembly", "/gibt/es/nicht.dll", "--out", Fixtures.TempDir());
        Assert.Equal(3, result.Exit);
        Assert.Contains("nicht gefunden", result.Error);
    }

    [Fact]
    public void UnreadableAssemblyGivesExitCode3()
    {
        string dir = Fixtures.TempDir();
        string bogus = Path.Combine(dir, "kaputt.dll");
        File.WriteAllText(bogus, "keine Assembly");
        var result = Run("--assembly", bogus, "--out", Path.Combine(dir, "out"));
        Assert.Equal(3, result.Exit);
        Assert.Contains("nicht lesbar", result.Error);
    }

    [Theory]
    [InlineData("--assembly x.dll")]
    [InlineData("--assembly")]
    [InlineData("--unbekannt 1 --assembly x --out y")]
    public void InvalidArgumentsGiveExitCode2(string commandLine)
    {
        Assert.Equal(2, Run(commandLine.Split(' ')).Exit);
    }
}
