using System;

namespace PortFrontendUnity;

/// <summary>Einstieg von <c>port-frontend-unity</c> (docs/modules/frontend-unity.md).</summary>
internal static class Program
{
    private static int Main(string[] args)
    {
        Options? options = Options.Parse(args, out string? error);
        if (options == null)
        {
            Console.Error.WriteLine(error);
            return Frontend.InvalidArguments;
        }
        try
        {
            return Frontend.Run(options, Console.Error);
        }
        catch (Exception e)
        {
            Console.Error.WriteLine("Absturz: " + e);
            return Frontend.TranslatorFailure;
        }
    }
}
