using ICSharpCode.Decompiler;
using ICSharpCode.Decompiler.CSharp;

namespace PortFrontendUnity;

/// <summary>
/// Dekompiler-Einstellungen: C# 7.3 (Unitys Mono-Profil) und Syntax-Zucker aus, damit möglichst wenig
/// aufgelöst werden muss (docs/ir-spec.md Abschnitt 1, „entzuckert"). Liste in docs/modules/frontend-unity.md.
/// </summary>
internal static class Settings
{
    public static DecompilerSettings Create()
    {
        var settings = new DecompilerSettings(LanguageVersion.CSharp7_3)
        {
            ThrowOnAssemblyResolveErrors = false,
            UseDebugSymbols = true,
            ShowXmlDocumentation = false,
            AlwaysUseBraces = true,
            StringInterpolation = false,
            NullPropagation = false,
            ObjectOrCollectionInitializers = false,
            UseExpressionBodyForCalculatedGetterOnlyProperties = false,
            SwitchExpressions = false,
            LocalFunctions = false,
            OutVariables = false,
            Discards = false,
            NamedArguments = false,
            OptionalArguments = false,
            ExpressionTrees = false,
        };
        return settings;
    }
}
