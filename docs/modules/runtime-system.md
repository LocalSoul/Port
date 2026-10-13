# Modul `runtime-system`

## Zweck
Java-Nachbau des .NET-Basisbibliothek-Ausschnitts (`System.*`), den übersetzter Spielcode benutzt. Der Generator bildet IR-Symbole über `mapping.tsv` auf dieses Modul ab (ADR-019).

## Abhängigkeiten
Keine zu anderen Projektmodulen. Nur JDK.

## Umfang (wächst bedarfsgesteuert)

Stand M3a (Paket `io.github.localsoul.runtime.system`, Unterpakete spiegeln .NET):
- `collections.generic`: `List<T>`, `Dictionary<K,V>`, `HashSet<T>` (Reihenfolge exakt wie .NET über Eintragsarray mit LIFO-Freiliste, package-private `Slots`), `Queue<T>`, `Stack<T>`, `KeyValuePair<K,V>`, `KeyNotFoundException`; alle `Iterable` mit .NET-Versionsprüfung beim Aufzählen.
- `Strings` (Gleichheit, `Concat`, `IsNullOrEmpty`, `Substring`), `Convert` (`ToString` der Grundtypen im Format von .NET Framework/Mono: G7/G15), `SystemObject` (`ReferenceEquals`, `Equals`, `ToString`), `Equality` (Gleichheit wie .NET, `0.0` gleich `-0.0`).
- `Math` (auf `double`, `Round` mit Banker's Rounding, `Abs(int.MinValue)` → `OverflowException`, `Clamp`), `MathF` (auf `float`, `StrictMath`).
- Exceptions: `SystemException` (für `System.Exception` und `System.SystemException`, erbt von `RuntimeException`) mit `message()` und .NET-Standardmeldungen, `ArgumentException`, `ArgumentNullException`, `ArgumentOutOfRangeException` (Parametername wie .NET), `InvalidOperationException`, `NotSupportedException`, `NullReferenceException`, `IndexOutOfRangeException`, `OverflowException`; `fromJava` übersetzt gefangene Java-Exceptions (siehe `docs/type-mapping.md`).
- `Func0`–`Func4`, `Action0`–`Action4` (Ziffer = Zahl der Parameter, Methode `invoke`).
- `src/main/resources/mapping.tsv`: Abbildung aller umgesetzten Symbole (Formen siehe `docs/modules/generator-java.md`, u. a. `helper` und Typzeilen mit Stelligkeit).

Weitere Typen kommen nur auf Bedarf (`port deps check` oder Lint SH001 meldet, was der Spielcode benutzt).

## Ab M10b
`NativeMemory`: flacher, little-endian Byte-Adressraum mit `load`/`store` für alle Primitive, Allokation, Prüfung der Grenzen, deterministisch. Importe nativer Funktionen (`malloc`, `memcpy`, ...) als Mapping in `mapping.tsv`.

## Regeln
- Namenskonvention und Abbildung laut `docs/type-mapping.md`.
- **Jeder Semantikunterschied ist ein Test.** Beispiele: `Dictionary`-Iterationsreihenfolge, Ausnahme bei fehlendem Schlüssel, `Math.Round` (Banker's Rounding), Index außerhalb des Bereichs, `Substring` mit ungültigen Grenzen.
- Keine Abhängigkeit auf Engine-Typen.
- Alles deterministisch (keine Abhängigkeit von `HashMap`-Reihenfolge).
- `mapping.tsv` und die Klassen müssen konsistent sein (Test).

## Tests
- Pro Klasse Verhaltenstests gegen dokumentiertes .NET-Verhalten (Erwartungswerte aus der .NET-Dokumentation, nicht aus der Java-Implementierung kopiert).
- Konsistenztest (`MappingConsistencyTest`): Zeilen wohlgeformt und sortiert, jedes Ziel existiert als Klasse bzw. Methode (Reflection nur im Test).
- ArchUnit: nur JDK, keine Reflection, keine Konsolenausgabe.
- End-to-End in `integration-tests` (`RuntimeSystemEndToEndTest`): Fixtures `Inventory`, `ControlFlow`, `Ledger` laufen gegen dieses Modul.

## Nicht-Ziele
Vollständiger Nachbau der .NET-Bibliothek, Reflection, `System.IO`, Netzwerk, Threading (außer was ADR-008 später verlangt).
