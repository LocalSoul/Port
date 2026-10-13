# M2: Unity-Frontend (C# → IR)

**Ziel:** Ein .NET-Programm, das eine Unity-Mono-Assembly in IR umwandelt, plus Java-Wrapper. Getestet mit synthetischen Fixtures, nicht mit echten Spielen.

**Eingaben:** `AGENTS.md`, `docs/ir-spec.md`, `docs/modules/frontend-unity.md`, `docs/modules/ir.md`, `docs/decisions.md` (ADR-003, ADR-005).

**Abhängigkeiten/Entscheidungen**
- M0 ist abgeschlossen (IR-Reader/Printer/Validator stehen).
- **ADR-005 (separater .NET-Prozess)** ist `PROPOSED`. Du darfst folgen und weist im Bericht darauf hin.
- Das .NET SDK muss vorhanden sein. Wenn nicht: sage es klar, überspringe nicht stillschweigend, und liefere zumindest die Fixtures und den Java-Wrapper mit Tests gegen vorab erzeugte Beispielausgabe.

---

## M2.1 Fixtures

**Schritte**
1. `testdata/unity-stubs/`: C#-Projekt `UnityEngine.Stubs` mit **nur den Signaturen**, die die Fixtures brauchen (`MonoBehaviour`, `Component`, `Transform`, `Vector3` als `struct` mit Operatoren, `Input`, `Time`, `Mathf` o. Ä.). Keine Implementierung.
2. `testdata/unity-sources/`: kleine Spielklassen (`Mover.cs` aus Spec-Beispiel, ein Zähler, eine Klasse mit `if`/`for`/`switch`, eine mit einem Feld und Property). Die Fixtures nutzen nur Primitive und `string` aus dem BCL, keine Collections (die kommen in M3a).
3. Skript oder MSBuild-Ziel, das daraus eine Test-Assembly `Assembly-CSharp.dll` baut (nicht eingecheckt, wird erzeugt).

**Abnahme**
- [x] `dotnet build testdata/...` erzeugt die Test-Assembly reproduzierbar.

## M2.2 Dekompilierung in IR, Grundumfang

**Schritte**
1. .NET-Konsolenprogramm `port-frontend-unity` (siehe `docs/modules/frontend-unity.md`), Aufruf `--assembly <dll> --out <ir>`.
2. Mit `ICSharpCode.Decompiler` Typen, Felder, Properties, Methoden, Konstruktoren auslesen.
3. Ausdrücke und Anweisungen nach IR (Spec Abschnitte 5 bis 7): `local`, `set`, `if`, `while`, `for`, `foreach`, `switch` (ohne Durchfallen), `return`, Aufrufe (`call`, `scall`, `new`), Operatoren, Felder, Properties, Literale mit Typsuffix.
4. Operatoren auf eigenen Typen als `scall ...::op_*` (Spec Regel Abschnitt 7).
5. Alle Referenzen voll aufgelöst als `mref` mit Signatur.

**Abnahme**
- [x] Für `Mover.cs` entsteht die IR aus Spec Abschnitt 10, nach Kanonisierung durch den Java-`IrPrinter` byte-gleich zum Golden File.
- [x] Für die weiteren Fixtures liegen Golden Files unter `testdata/golden/ir/`.
- [x] Die erzeugte IR besteht den Validator (keine IR001–IR005).
- [x] Zwei Läufe erzeugen bytegleiche Ausgabe (Determinismus).

## M2.3 Nicht übersetzbare Konstrukte

**Schritte**
1. Unbekannte oder nicht unterstützte Konstrukte (z. B. `unsafe`, komplexes LINQ) werden zu `(raw cs "<Originalcode>")` und das Member bekommt `:manual "<Grund>"`.
2. Das Programm bricht nicht ab, es meldet die Anzahl auf stderr.

**Abnahme**
- [x] Eine Fixture mit `unsafe`-Block erzeugt `raw` und `:manual`, der Rest der Klasse wird normal übersetzt.
- [x] Das Programm endet mit Exit-Code 0 und einer Zusammenfassung, wenn nur `raw`-Stellen auftreten (die Behandlung als Fehler ist Sache des CLI-Vertrags, Exit 4 nur bei echten Abstürzen/Unlesbarkeit).

## M2.4 Java-Wrapper

**Schritte**
1. Modul `frontend-unity/java`: Findet das .NET-Programm (Konfiguration, sonst `PATH`), startet den Prozess, liest die Ausgabe, parst mit `IrReader`, druckt kanonisch.
2. Fehlerabbildung: Prozessfehler → `FrontendException` mit Exit-Code des CLI-Vertrags (Nutzerentscheidung 2026-10-13; `PortException` gibt es erst in `port-cli`, M4).

**Abnahme**
- [x] Wrapper-Test mit vorab erzeugter IR-Datei (ohne .NET), prüft Kanonisierung.
- [x] Integrationstest mit .NET-Programm läuft nur, wenn `dotnet` vorhanden ist, sonst wird er übersprungen und im Testbericht als übersprungen sichtbar (Assumption).
- [x] Ungültige Ausgabe des Programms führt zu klarer Fehlermeldung mit Position.

## M2.5 Abschluss
- [x] `docs/modules/frontend-unity.md` entspricht dem Stand.
- [x] Bekannte Lücken (nicht unterstützte Konstrukte) stehen als Liste in `PROGRESS.md`.

## M2.6 Probelauf mit eigenem Spiel (vom Nutzer ergänzt, 2026-10-13)

**Schritte**
1. Spiel-Assembly des eigenen Builds „StarCollector" unter `testdata/starcollector/` einchecken, den vollständigen Build (`testdata/Builds/`) ignorieren.
2. Option `--references` für Unity-DLLs; Probelauf als Test mit Abdeckungsbericht, ohne Golden File.

**Abnahme**
- [x] Übersetzung läuft mit Exit 0, jede Ausgabedatei ist gültige IR, keine IR001/IR004/IR005.
- [x] Bericht `frontend-unity/java/target/starcollector-report.txt`; Lücken in `PROGRESS.md`.

## Nicht-Ziele
IL2CPP, Szenen/Prefabs, Optimierung des IR, Godot-Frontend.
