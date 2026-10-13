using System;
using System.IO;

namespace PortFrontendUnity.Tests;

/// <summary>Pfade zu den Test-Fixtures (gebaut von scripts/build-dotnet.sh).</summary>
internal static class Fixtures
{
    public static string RepoRoot()
    {
        var directory = new DirectoryInfo(AppContext.BaseDirectory);
        while (directory != null && !Directory.Exists(Path.Combine(directory.FullName, "testdata", "unity-sources")))
        {
            directory = directory.Parent;
        }
        if (directory == null)
        {
            throw new InvalidOperationException("Repo-Wurzel mit testdata/unity-sources nicht gefunden");
        }
        return directory.FullName;
    }

    /// <summary>Fixture-Assembly; fehlt sie, zuerst scripts/build-dotnet.sh ausführen.</summary>
    public static string Assembly()
    {
        string path = Path.Combine(RepoRoot(), "testdata", "unity-sources", "bin", "Release", "netstandard2.1", "Assembly-CSharp.dll");
        if (!File.Exists(path))
        {
            throw new InvalidOperationException("Fixture fehlt: " + path + " (scripts/build-dotnet.sh ausführen)");
        }
        return path;
    }

    public static string TempDir()
    {
        string path = Path.Combine(Path.GetTempPath(), "port-frontend-unity-tests", Guid.NewGuid().ToString("N"));
        Directory.CreateDirectory(path);
        return path;
    }
}
