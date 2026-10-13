using ICSharpCode.Decompiler.CSharp;
using ICSharpCode.Decompiler.CSharp.Syntax;
using ICSharpCode.Decompiler.Semantics;

namespace PortFrontendUnity;

/// <summary>Null-sichere Zugriffe auf Syntaxknoten (Teilknoten sind in der Bibliothek nullable).</summary>
internal static class AstExtensions
{
    /// <summary>Semantik-Annotation; fehlender Knoten ergibt einen Fehler-ResolveResult.</summary>
    public static ResolveResult Rr(this AstNode? node)
    {
        return node == null ? ErrorResolveResult.UnknownError : node.GetResolveResult();
    }

    /// <summary>Verlangt einen Teilknoten, sonst nicht übersetzbar.</summary>
    public static T Required<T>(this T? node, string what)
        where T : AstNode
    {
        if (node == null)
        {
            throw new UnsupportedConstructException("fehlender Teilknoten: " + what);
        }
        return node;
    }
}
