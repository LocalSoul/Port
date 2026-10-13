# Typabbildung IR → Java (ADR-019, ACCEPTED; Unsigned-Teilfrage OPEN)

Verbindliche Tabelle für den Generator. Abweichungen gehören nach `docs/decisions.md`. Die Lint-Regeln SM001–SM009 (siehe `docs/cli-contract.md`) warnen vor den Stellen, an denen sich das Verhalten unterscheidet.

## Primitive

| IR | Java | Hinweise |
|---|---|---|
| `bool` | `boolean` | |
| `char` | `char` | beide sind UTF-16-Code-Units |
| `i8` | `byte` | |
| `i16` | `short` | |
| `i32` | `int` | Überlauf wie in C# `unchecked` (Zweierkomplement); `checked` → `Math.addExact` usw. |
| `i64` | `long` | |
| `f32` | `float` | |
| `f64` | `double` | |
| `u8`, `u16`, `u32`, `u64` | **OPEN** | Teilfrage in ADR-019. Generator erzeugt `todo`, Lint SM002 |
| `string` | `String` | Gleichheit ist ordinal. Vergleiche (`Compare`, `StartsWith` ohne Angabe) sind in C# kulturabhängig, siehe SM006 |
| `object` | `Object` | |
| `void` | `void` | |

## Zusammengesetzte Typen

| IR | Java | Hinweise |
|---|---|---|
| `T[]` | `T[]` | Index außerhalb → Exception, Abbildung siehe Exceptions |
| `T[,]`, `T[,,]` | **OPEN** | Vorschlag: Klasse `Array2D<T>`/`Array3D<T>` in `runtime-system` (SM008) |
| `T?` für Werttypen | Box-Typ (`Integer`, `Float`, ...) mit `null` | für Referenztypen ist `?` ohne Bedeutung |
| `struct` (eigene) | Klasse mit Kopierverhalten (ADR-009 OPEN) | Lint SM001 |
| `enum` | Java-`enum` mit `value()` und `fromValue(int)` | C#-Enums sind Zahlen, Werte außerhalb der Liste sind erlaubt: Vorschlag: Hilfsklasse für unbekannte Werte, bis zur Entscheidung `todo` |
| Delegates, `Action`, `Func` | funktionale Interfaces `Action0`–`Action4`, `Func0`–`Func4` in `runtime-system` (Ziffer = Zahl der Parameter, Aufruf `invoke`) | Mapping-Typzeilen mit Stelligkeit (`System.Func/2` → `Func1`); Lambdas erzeugt das Frontend noch nicht (`raw`). Events mit mehreren Subscribern: Lint PT007 |
| Generics | Java-Generics | Werttypen als Typargument werden geboxt (`List<int>` → `List<Integer>`) |

## Bibliothekstypen (`System.*`)

Java-Implementierungen liegen in `runtime-system` (siehe `docs/modules/runtime-system.md`), Paket spiegelt den .NET-Namespace: `io.github.localsoul.runtime.system.collections.generic.List`. Methoden und Properties laut Namenskonvention unten.

| .NET | `runtime-system` | Semantikunterschiede, die dokumentiert und getestet werden |
|---|---|---|
| `List<T>` | `List<T>` | Indexer (`get`/`set`), `RemoveAt`, `Insert` werfen `ArgumentOutOfRangeException`; `count()`; Änderung während `foreach` → `InvalidOperationException` (Versionszähler wie .NET); Gleichheit wie .NET (`0.0f` gleich `-0.0f`) |
| `Dictionary<K,V>` | `Dictionary<K,V>` | Reihenfolge exakt wie die .NET-Referenzimplementierung (Nutzerentscheidung M3a): Eintragsarray mit LIFO-Freiliste, nach `Remove` füllt das nächste `Add` die zuletzt frei gewordene Stelle. Fehlender Schlüssel → `KeyNotFoundException`, doppelter → `ArgumentException`, `null`-Schlüssel → `ArgumentNullException`; Überschreiben während `foreach` wirft (wie Mono) |
| `HashSet<T>` | `HashSet<T>` | Reihenfolge wie `Dictionary` |
| `Queue<T>`, `Stack<T>` | gleiche Namen | |
| `string`-Methoden | statische Hilfen in `Strings` (Mapping-Form `helper`, Empfänger als erstes Argument) | `==`/`!=` ordinal und null-sicher (`opEquality`), `Concat` mit `null` als „“ und Zahlen im .NET-Format, `Substring(start, länge)` mit `ArgumentOutOfRangeException`. Kultur/Ordinal bei Vergleichen (SM006) |
| `ToString()` der Grundtypen | `Convert.toString` | siehe Zahlformat unten |
| `System.Math` / `Mathf` | `Math` (System) bzw. Adapter (`Mathf`) | `Math.Round` rundet standardmäßig auf gerade Zahl (Banker's Rounding), Java `Math.round` nicht. Umsetzung mit `Math.rint`. Division durch 0 bei Ganzzahlen wirft in beiden Welten, bei Gleitkomma nicht |
| `System.Random` | `Random` | Sequenzen sind **nicht** identisch zu .NET. Wenn Spiele deterministisch auf Seeds angewiesen sind, ist das ein Portierungsrisiko (Lint-Hinweis später) |
| `System.DateTime`, `TimeSpan` | `DateTime`, `TimeSpan` | nur nach Bedarf |
| `System.Linq` | **nur einfache Fälle** | Komplexes LINQ: Lint PT004, Handarbeit oder KI-Fix |

## Exceptions

| .NET | Java (`runtime-system`) |
|---|---|
| `System.Exception` | `SystemException` (erbt von `RuntimeException`) |
| `ArgumentException`, `ArgumentNullException`, `ArgumentOutOfRangeException` | gleiche Namen, erben von `SystemException` |
| `NullReferenceException` | gleicher Name; Java-`NullPointerException` wird im generierten `catch` dazu übersetzt |
| `IndexOutOfRangeException` | gleicher Name; aus `ArrayIndexOutOfBoundsException` übersetzt |
| `InvalidOperationException`, `NotSupportedException`, `OverflowException` | gleiche Namen |
| `KeyNotFoundException` | gleicher Name (Paket `collections.generic`) |

- `message()` (`Exception.Message`) liefert die übergebene Meldung oder die englische .NET-Standardmeldung des Typs („Value cannot be null.“, „Object reference not set to an instance of an object.“ usw.).
- Wie in .NET ist das einzelne String-Argument von `ArgumentNullException` und `ArgumentOutOfRangeException` der **Parametername**; `Message` lautet dann „Meldung\nParameter name: x“ (Format von .NET Framework/Mono, Zeilenumbruch immer `\n`).
- `catch (System.Exception e)` wird zu `catch (SystemException | NullPointerException | ArrayIndexOutOfBoundsException e$) { SystemException e = SystemException.fromJava(e$); ... }`, `catch (NullReferenceException)` fängt zusätzlich nur `NullPointerException`, `catch (IndexOutOfRangeException)` nur `ArrayIndexOutOfBoundsException`. Andere Java-Exceptions (z. B. `ArithmeticException` bei Ganzzahl-Division durch 0) werden noch nicht übersetzt.

## Zahlformat (`ToString`, Verkettung)

Wie .NET Framework / Unity-Mono, invariant (Nutzerentscheidung M3a): `float` mit „G7“, `double` mit „G15“, kaufmännisch gerundet, ohne nachgestellte Nullen. Exponentenform, wenn der Exponent ≥ Stellenzahl oder < −4 ist (`1E-05`, `1.234568E+07`, `0.0001` bleibt). `-0` → „0“, `NaN`, `Infinity`, `-Infinity`; `bool` → „True“/„False“. Offen: Abgleich mit einem echten Unity-Lauf.

## Namenskonvention (gilt für `runtime-system` und Adapter)

- Typen behalten den Namen, Methoden und Felder werden `lowerCamelCase`.
- Properties → Methode `name()` und `setName(v)`, `bool`-Properties behalten ihren Namen (`isActive()` nur, wenn das Original `IsActive` heißt: dann `isActive()`).
- Indexer → `get(i)` und `set(i, v)`.
- Operatoren → benannte statische Methoden: `op_Addition` → `add`, `op_Subtraction` → `sub`, `op_Multiply` → `mul`, `op_Division` → `div`, `op_Equality` → `equals`, `op_Inequality` → `notEquals`, `op_UnaryNegation` → `neg`.
- Events → `addName(handler)` und `removeName(handler)`.
- Statische Klassen → `final` Klasse mit privatem Konstruktor.
- Konflikte mit Java-Schlüsselwörtern (`default`, `new`, `class` als Methodenname) → Suffix `_` (`default_`).

## Mapping-Datei

Jedes Modul, das Typen bereitstellt (`runtime-system`, jeder Adapter), liefert `mapping.tsv` mit Spalten `IR-Symbol<TAB>Java-Symbol<TAB>Form` (Formen siehe `docs/modules/generator-java.md`). Fehlt ein Symbol, gilt es als `missing` (Lint SH001). Die Datei ist sortiert nach IR-Symbol und deterministisch. Ein Status `full`/`stub` für Lint ist noch nicht umgesetzt.

## Beispiel

```
System.Collections.Generic.List	io.github.localsoul.runtime.system.collections.generic.List	type
System.Collections.Generic.List::Add#T	add	method
System.Collections.Generic.List::Count	count	getter
System.Func/2	io.github.localsoul.runtime.system.Func1	type
System.String::Substring#i32,i32	substring	helper
UnityEngine.Vector3::op_Multiply#UnityEngine.Vector3,f32	mul	operator
```
