using System;
using System.Collections.Generic;
using System.Reflection.Metadata;
using System.Reflection.PortableExecutable;
using ICSharpCode.Decompiler.DebugInfo;

namespace PortFrontendUnity;

/// <summary>
/// Liest Namen lokaler Variablen aus einer eingebetteten Portable PDB. Ohne PDB (übliche Unity-Builds)
/// vergibt der Dekompiler deterministische Namen.
/// </summary>
internal sealed class EmbeddedPdb : IDebugInfoProvider
{
    private readonly MetadataReader pdb;

    private EmbeddedPdb(MetadataReader pdb)
    {
        this.pdb = pdb;
    }

    /// <summary>Liefert den Provider oder <c>null</c>, wenn keine eingebettete PDB vorhanden ist.</summary>
    public static IDebugInfoProvider? TryLoad(PEReader reader)
    {
        foreach (DebugDirectoryEntry entry in reader.ReadDebugDirectory())
        {
            if (entry.Type == DebugDirectoryEntryType.EmbeddedPortablePdb)
            {
                MetadataReaderProvider provider = reader.ReadEmbeddedPortablePdbDebugDirectoryData(entry);
                return new EmbeddedPdb(provider.GetMetadataReader());
            }
        }
        return null;
    }

    public string Description => "eingebettete Portable PDB";

    public string SourceFileName => "embedded";

    public IList<ICSharpCode.Decompiler.DebugInfo.SequencePoint> GetSequencePoints(MethodDefinitionHandle method) =>
        Array.Empty<ICSharpCode.Decompiler.DebugInfo.SequencePoint>();

    public IList<Variable> GetVariables(MethodDefinitionHandle method) => Array.Empty<Variable>();

    public bool TryGetName(MethodDefinitionHandle method, int index, out string name)
    {
        foreach (LocalScopeHandle scopeHandle in pdb.GetLocalScopes(method))
        {
            LocalScope scope = pdb.GetLocalScope(scopeHandle);
            foreach (LocalVariableHandle variableHandle in scope.GetLocalVariables())
            {
                LocalVariable variable = pdb.GetLocalVariable(variableHandle);
                if (variable.Index == index)
                {
                    name = pdb.GetString(variable.Name);
                    return true;
                }
            }
        }
        name = null!;
        return false;
    }

    public bool TryGetExtraTypeInfo(MethodDefinitionHandle method, int index, out PdbExtraTypeInfo extraTypeInfo)
    {
        extraTypeInfo = default;
        return false;
    }
}
