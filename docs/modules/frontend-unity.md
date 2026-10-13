# Modul `frontend-unity`

## Zweck
Wandelt Unity-Mono-Assemblies in IR um. Besteht aus einem .NET-Teil (Dekompilierung) und einem Java-Wrapper (Prozessstart, Einlesen).

## Aufbau
```
frontend-unity/
  dotnet/            .NET-Konsolenprogramm `port-frontend-unity` (ICSharpCode.Decompiler, optional Roslyn)
    src/  tests/
  java/              Wrapper: startet den Prozess, liest IR-Text, validiert mit `ir`
```
Entscheidung ADR-005 (PROPOSED): separater Prozess, Schnittstelle über Datei oder stdout.

## .NET-Programm
Aufruf: `port-frontend-unity --assembly <dll> --out <ir-datei> [--classes <glob>]`
- Liest die Assembly, geht die Typen durch, erzeugt IR nach `docs/ir-spec.md`.
- **Auflösen:** Jede Referenz wird zu einer `mref` mit voller Signatur. Operatoren auf eigenen Typen werden zu `scall ...::op_*`. Syntax-Zucker wird in explizite Knoten aufgelöst (siehe IR-Spec Abschnitt 1).
- **Nicht übersetzbare Konstrukte:** `(raw cs "...")` mit dem Original-C#, am Member `:manual "<Grund>"`. Nie still verwerfen.
- **Deterministische Ausgabe:** kanonische Reihenfolge, keine Zeitstempel/Pfade. Der Java-`IrPrinter` ist maßgeblich; das .NET-Programm kann Rohausgabe liefern, die der Java-Wrapper kanonisiert.
- Exit-Code ungleich 0 bei Fehlern, Fehlermeldungen auf stderr.

## Java-Wrapper
- Findet das .NET-Programm (Konfiguration oder `PATH`), startet es, liest die Ausgabe, parst sie mit `IrReader`, druckt kanonisch.
- Übersetzt .NET-Fehler in `PortException` mit Exit-Code 4 bzw. 3 laut CLI-Vertrag.

## Tests und Fixtures
- **Keine echten Spiele.** Fixtures sind kleine C#-Quellen unter `testdata/unity-sources/` (z. B. `Mover.cs`), kompiliert gegen **eigene Stub-Assemblies** `UnityEngine.Stubs` (nur die benötigten API-Signaturen, ohne Implementierung).
- Golden Files: `testdata/golden/ir/Mover.ir` und weitere.
- Fehlt das .NET SDK, werden die Frontend-Tests übersprungen und das im Bericht klar benannt.

## Nicht-Ziele
- Kein IL2CPP in diesem Modul (ADR-003). Nativer Code gehört zu `frontend-native` (ADR-023).
- Keine Extraktion von Szenen/Prefabs in M2 (kommt in M8).
- Keine Optimierung des IR, nur treue Abbildung.
