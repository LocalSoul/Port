# Modul `frontend-unity`

## Zweck
Wandelt Unity-Mono-Assemblies in IR um. Besteht aus einem .NET-Programm (Dekompilierung, ADR-005) und einem Java-Wrapper (Prozessstart, Kanonisierung).

## Aufbau
```
frontend-unity/
  dotnet/                       PortFrontendUnity.sln, getrennt vom Maven-Build gebaut
    src/PortFrontendUnity/      Programm port-frontend-unity (net8.0, ICSharpCode.Decompiler 11.1.0.9782)
    tests/PortFrontendUnity.Tests/  xUnit
  java/                         Maven-Modul frontend-unity (hängt nur von ir ab)
testdata/unity-stubs/           UnityEngine-Stubs (nur Signaturen), Assembly "UnityEngine"
testdata/unity-sources/         Fixtures, Assembly "Assembly-CSharp" (netstandard2.1, eingebettete PDB)
testdata/starcollector/         Spiel-Assembly des Nutzers für den Probelauf (siehe README dort)
testdata/golden/ir/unity/       Golden Files je Fixture-Typ
scripts/build-dotnet.sh         baut Fixtures (zweimal, Hash-Vergleich), Programm (Release), führt xUnit aus
```
Build: `scripts/build-dotnet.sh`, danach `./mvnw verify`. Ohne gebautes Programm oder ohne `dotnet` werden die Java-Integrationstests sichtbar übersprungen (JUnit-Assumptions).

## .NET-Programm

Aufruf: `port-frontend-unity --assembly <dll> --out <verzeichnis> [--classes <glob>]... [--references <verzeichnis>]... [--no-sha256]`

- **Ausgabe:** eine Datei je Typ, `<qualifizierter Name>.ir`, jede mit Kopf `(ir :version 1 :assembly "<Name>" [:sha256 "<hex>"])` (Nutzerentscheidung M2). Rohformat; kanonisch macht der Java-`IrPrinter`.
- `--classes`: Muster auf den vollen Typnamen (`*`, `?`, mehrfach oder kommagetrennt).
- `--references`: zusätzliche Suchverzeichnisse für referenzierte Assemblies (Default: Verzeichnis der Assembly). Nicht auflösbare Typen werden `unknown` (IR003).
- `--no-sha256`: ohne Hash, für Golden-Tests (der Hash der Test-Assembly hängt an der Compiler-Version).
- **Exit-Codes** (`docs/cli-contract.md`): 0 Erfolg, auch mit `raw`-Stellen; 2 ungültige Argumente; 3 Assembly fehlt oder ist unlesbar; 4 Absturz des Dekompilers bei mindestens einem Typ (die übrigen Typen werden trotzdem geschrieben).
- **stderr:** je Lücke eine Zeile (`raw: Typ::Member: Grund`, `übersprungen: ...`, `hinweis: ...`) und eine Zusammenfassung `N Typen, M Mitglieder, R raw-Stellen, S übersprungene Typen`.
- **Lokale Namen:** aus einer eingebetteten Portable PDB, wenn vorhanden (Fixtures); sonst vergibt der Dekompiler deterministische Namen (übliche Unity-Builds).

### Dekompiler-Einstellungen (`Settings.cs`)
C# 7.3 (Unitys Mono-Profil), `UseDebugSymbols`, `AlwaysUseBraces`. Abgeschaltet: String-Interpolation, `?.`, Objekt-/Collection-Initialisierer, Ausdrucksrümpfe für Getter, Switch-Ausdrücke, lokale Funktionen, `out var`, Discards, benannte und optionale Argumente (alle Argumente stehen explizit), Expression Trees.

### Abbildungsregeln
- Grundlage ist der C#-Syntaxbaum mit Semantik-Annotationen (`GetResolveResult`). Fehlt einem verschachtelten Knoten die Annotation (Teile von Verkettungen, konstant gefaltete Ausdrücke), wird der Typ aus Operanden bzw. dem .NET-Wert des Literals abgeleitet.
- **Typen:** Primitive nach Spec Abschnitt 3, Arrays, `T?`, `T&`, Generics. Zeiger, `IntPtr`, `decimal`, `dynamic` sind nicht abbildbar.
- **Namen:** Unzulässige Zeichen in Bezeichnern werden deterministisch als `_uXXXX` kodiert (Umlaute, `<` in Compiler-Namen).
- **Literale:** immer mit Typsuffix; `float`/`double` im kürzesten Rundreiseformat mit `.0` bzw. kleinem `e`; NaN/Unendlich als `(sfield System.Single::NaN)` usw.; `char` als `(char N)`.
- **Operatoren:** auf Primitiven `opsym`; eigene Operatoren (`Vector3 * float`) und `string ==` als `scall ...::op_*`; String-Verkettung paarweise als `System.String::Concat#string,string` bzw. `#object,object`; `==`/`!=` zwischen Referenztypen ohne eigenen Operator als `System.Object::ReferenceEquals`. Implizite numerische Umwandlungen werden zu `(conv T x)`.
- **Zuweisungen:** `x++`/`++x` → `post-inc`/`pre-inc`; `+=` auf Primitiven → `set-op`, auf `string` → `set` mit `Concat`.
- **Member:** Implizites `this` wird explizit. Feldinitialisierer stehen am Feld, triviale Default-Konstruktoren entfallen (macht der Dekompiler). `: base(...)` wird `:base-call`.
- **Auto-Properties** (Nutzerentscheidung M2): privates Feld `__auto_<Name>` plus `:get (return (field this T::__auto_<Name>))` und `:set (block (expr (set ... value)))`.
- **Modifier:** Zugriff (`protected internal`, `private protected` als zwei Modifier), `static`, `abstract`, `virtual`, `override`, `sealed`, `readonly`, `const`.
- **Attribute:** an Typen und Feldern mit positionalen Argumenten. Compiler-Attribute (`System.Runtime.CompilerServices`, `System.Diagnostics`, ...) entfallen. Attribute an Methoden entfallen mit Hinweis (Spec-Lücke).
- **Release-Code wird treu abgebildet:** Der Compiler formt Code um (z. B. `switch` über wenige Strings → `if`-Kette, `continue` → umgedrehtes `if`). Die IR zeigt die kompilierte Form.

### Nicht übersetzbare Konstrukte (M2.3)
- Ein Statement wird zu `(raw cs "<Originaltext>")`, ein Initialisierer zu `(raw cs "...")` als Ausdruck. Das Member bekommt `:manual "<Grund>"`, der Rest der Klasse wird normal übersetzt.
- Ein Member, das sich gar nicht abbilden lässt (z. B. Indexer, Destruktor), wird zu `(method <Name> void () :manual "<Grund>" (raw cs "<Deklaration>"))`.
- Verschachtelte Typen werden ausgelassen und auf stderr gemeldet (Spec-Lücke).
- Nie still verwerfen: Jede Lücke erscheint auf stderr und wird gezählt.

## Java-Wrapper (`io.github.localsoul.port.frontend.unity`)
- `UnityFrontend.translate(TranslationRequest)` → `TranslationResult` mit `IrFile` und kanonischem Text je Typ (sortiert) sowie der stderr-Zusammenfassung.
- Programm: Systemproperty `port.frontend.unity`, sonst Umgebungsvariable `PORT_FRONTEND_UNITY`, sonst `port-frontend-unity` im `PATH`. Pfade auf `.dll` werden mit `dotnet` gestartet. Zeitlimit 5 Minuten.
- `ToolRunner` ist austauschbar (Standard `ProcessToolRunner`, Tests `FakeToolRunner`).
- **Fehler:** `FrontendException` mit Exit-Code laut CLI-Vertrag. Programm-Exit 3 → 3, anderer Exit ≠ 0 → 4, ungültige IR → 4 mit `datei:zeile:spalte`, Programm nicht startbar → 1. Das CLI übersetzt sie ab M4 in `PortException`.

## Tests und Fixtures
- **Fixtures** (`testdata/unity-sources/`): `Mover` (Spec-Beispiel), `Counter`, `ControlFlow`, `Holder`, `UnsafeBits` (`fixed`-Block → `raw`), `Edges` (gefaltete Konstanten, Lambda-Initialisierer). Gebaut gegen die eigenen Stubs, reproduzierbar (Hash-Vergleich im Build-Skript), eingebettete PDB für echte Variablennamen.
- **xUnit:** Literale, String-Escapes, Namen, Globs, Ende-zu-Ende gegen die Fixtures (Dateien, PDB-Namen, `raw`, Determinismus, Exit-Codes 2 und 3).
- **Java:**
  - Wrapper-Tests ohne .NET.
  - Integrationstest: Golden Files unter `testdata/golden/ir/unity/` (`-Dgolden.update=true`), Validator ohne `error`, Determinismus, Klassenfilter.
  - `Mover` gleich `testdata/golden/ir/spec-example.ir` bis auf `:sha256` und `:mods (public)` an der Klasse. Die Modifier stehen in der Assembly, das Spec-Beispiel lässt sie weg.
- **Probelauf** `StarCollectorSmokeTest`: eigene Spiel-Assembly, mit den lokalen Unity-DLLs aus `testdata/Builds/` falls vorhanden. Prüft auf Strukturfehler (IR001/IR004/IR005) und schreibt `frontend-unity/java/target/starcollector-report.txt`. Kein Golden File.

## Nicht-Ziele
- Kein IL2CPP in diesem Modul (ADR-003). Nativer Code gehört zu `frontend-native` (ADR-023).
- Keine Extraktion von Szenen/Prefabs in M2 (kommt in M8).
- Keine Optimierung des IR, nur treue Abbildung.
