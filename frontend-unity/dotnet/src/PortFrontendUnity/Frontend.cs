using System;
using System.IO;
using System.Linq;
using System.Security.Cryptography;
using ICSharpCode.Decompiler.CSharp;
using ICSharpCode.Decompiler.CSharp.Syntax;
using ICSharpCode.Decompiler.Metadata;
using ICSharpCode.Decompiler.TypeSystem;

namespace PortFrontendUnity;

/// <summary>
/// Übersetzt eine Assembly in IR, eine Datei je Typ (<c>&lt;qualifizierter Name&gt;.ir</c>). Exit-Codes
/// nach docs/cli-contract.md: 0 ok (auch mit raw-Stellen), 3 Eingabe fehlt/unlesbar, 4 Dekompiler-Absturz
/// bei mindestens einem Typ (die übrigen Typen werden trotzdem geschrieben).
/// </summary>
internal static class Frontend
{
    public const int Ok = 0;
    public const int InvalidArguments = 2;
    public const int InputNotFound = 3;
    public const int TranslatorFailure = 4;

    public static int Run(Options options, TextWriter error)
    {
        if (!File.Exists(options.Assembly))
        {
            error.WriteLine("Assembly nicht gefunden: " + options.Assembly);
            return InputNotFound;
        }
        PEFile file;
        try
        {
            file = new PEFile(options.Assembly);
        }
        catch (Exception e) when (e is BadImageFormatException or MetadataFileNotSupportedException)
        {
            error.WriteLine("Assembly nicht lesbar: " + options.Assembly + ": " + e.Message);
            return InputNotFound;
        }
        using (file)
        {
            return Translate(file, options, error);
        }
    }

    private static int Translate(PEFile file, Options options, TextWriter error)
    {
        var resolver = new UniversalAssemblyResolver(options.Assembly, false, file.DetectTargetFrameworkId());
        foreach (string reference in options.References)
        {
            resolver.AddSearchDirectory(reference);
        }
        var decompiler = new CSharpDecompiler(file, resolver, Settings.Create())
        {
            DebugInfoProvider = EmbeddedPdb.TryLoad(file.Reader),
        };
        Directory.CreateDirectory(options.OutDir);
        string header = Header(file, options);
        var report = new Report();
        bool crashed = false;
        var types = decompiler.TypeSystem.MainModule.TypeDefinitions
            .Where(t => t.DeclaringTypeDefinition == null && !t.Name.StartsWith('<'))
            .OrderBy(t => t.ReflectionName, StringComparer.Ordinal)
            .ToList();
        foreach (ITypeDefinition type in types)
        {
            if (!Glob.MatchesAny(options.Classes, type.ReflectionName))
            {
                continue;
            }
            crashed |= !TranslateType(decompiler, type, header, options.OutDir, report, error);
        }
        report.WriteTo(error);
        return crashed ? TranslatorFailure : Ok;
    }

    /// <summary>Übersetzt einen Typ; <c>false</c> nur bei einem unerwarteten Fehler des Dekompilers.</summary>
    private static bool TranslateType(
        CSharpDecompiler decompiler, ITypeDefinition type, string header, string outDir, Report report, TextWriter error)
    {
        try
        {
            SyntaxTree tree = decompiler.DecompileType(type.FullTypeName);
            TypeDeclaration declaration = tree.Descendants.OfType<TypeDeclaration>().First();
            Sx translated = new DeclTranslator(report).Type(type, declaration);
            string name = TypeNames.QualifiedName(type);
            File.WriteAllText(Path.Combine(outDir, name + ".ir"), header + "\n\n" + S.Render(translated) + "\n");
            report.TypeTranslated();
            return true;
        }
        catch (UnsupportedConstructException e)
        {
            report.SkippedType(type.ReflectionName, e.Reason);
            return true;
        }
        catch (Exception e) when (e is not OutOfMemoryException)
        {
            // Kein Verschlucken: Meldung auf stderr, Exit-Code 4 am Ende (docs/cli-contract.md).
            error.WriteLine("Fehler bei " + type.ReflectionName + ": " + e.GetType().Name + ": " + e.Message);
            return false;
        }
    }

    private static string Header(PEFile file, Options options)
    {
        var items = new System.Collections.Generic.List<Sx>
        {
            S.A("ir"), S.A(":version"), S.A("1"), S.A(":assembly"), S.Str(file.Name),
        };
        if (options.IncludeSha256)
        {
            using var stream = File.OpenRead(options.Assembly);
            string hash = Convert.ToHexString(SHA256.HashData(stream)).ToLowerInvariant();
            items.Add(S.A(":sha256"));
            items.Add(S.Str(hash));
        }
        return S.Render(S.L(items));
    }
}
