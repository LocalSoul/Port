using System.IO;
using ICSharpCode.Decompiler.CSharp.OutputVisitor;
using ICSharpCode.Decompiler.CSharp.Syntax;

namespace PortFrontendUnity;

/// <summary>Original-C#-Text eines Knotens für <c>(raw cs "...")</c>.</summary>
internal static class CSharpText
{
    public static string Of(AstNode node)
    {
        var writer = new StringWriter();
        var tokens = new TextWriterTokenWriter(writer) { IndentationString = "    " };
        var visitor = new CSharpOutputVisitor(tokens, FormattingOptionsFactory.CreateAllman());
        node.AcceptVisitor(visitor);
        return writer.ToString().Replace("\r\n", "\n").Trim();
    }
}
