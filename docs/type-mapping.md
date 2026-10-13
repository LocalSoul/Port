# Typabbildung IR → Java (ADR-019, PROPOSED)

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
| Delegates, `Action`, `Func` | funktionale Interfaces in `runtime-system` | Events mit mehreren Subscribern: Lint PT007 |
| Generics | Java-Generics | Werttypen als Typargument werden geboxt (`List<int>` → `List<Integer>`) |

## Bibliothekstypen (`System.*`)

Java-Implementierungen liegen in `runtime-system` (siehe `docs/modules/runtime-system.md`), Paket spiegelt den .NET-Namespace: `io.github.localsoul.runtime.system.collections.generic.List`. Methoden und Properties laut Namenskonvention unten.

| .NET | `runtime-system` | Semantikunterschiede, die dokumentiert und getestet werden |
|---|---|---|
| `List<T>` | `List<T>` | Indexer wirft bei ungültigem Index; `Count` statt `size`; Kapazitätsverhalten irrelevant |
| `Dictionary<K,V>` | `Dictionary<K,V>` | Iterationsreihenfolge: C# liefert bei Einfügen ohne Entfernen die Einfügereihenfolge. Umsetzung mit Einfügereihenfolge, bei Entfernen dokumentiertes Verhalten; Schlüsselzugriff auf fehlenden Schlüssel wirft |
| `HashSet<T>` | `HashSet<T>` | Reihenfolge wie `Dictionary` |
| `Queue<T>`, `Stack<T>` | gleiche Namen | |
| `string`-Methoden | statische Hilfen oder `StringExtensions` | Kultur/Ordinal (SM006), `Substring` wirft bei ungültigen Grenzen |
| `System.Math` / `Mathf` | `Math` (System) bzw. Adapter (`Mathf`) | `Math.Round` rundet standardmäßig auf gerade Zahl (Banker's Rounding), Java `Math.round` nicht. Umsetzung mit `Math.rint`. Division durch 0 bei Ganzzahlen wirft in beiden Welten, bei Gleitkomma nicht |
| `System.Random` | `Random` | Sequenzen sind **nicht** identisch zu .NET. Wenn Spiele deterministisch auf Seeds angewiesen sind, ist das ein Portierungsrisiko (Lint-Hinweis später) |
| `System.DateTime`, `TimeSpan` | `DateTime`, `TimeSpan` | nur nach Bedarf |
| `System.Linq` | **nur einfache Fälle** | Komplexes LINQ: Lint PT004, Handarbeit oder KI-Fix |

## Exceptions

| .NET | Java (`runtime-system`) |
|---|---|
| `System.Exception` | `SystemException` (erbt von `RuntimeException`) |
| `ArgumentException`, `ArgumentNullException`, `ArgumentOutOfRangeException` | gleiche Namen, erben von `SystemException` |
| `NullReferenceException` | wird aus `NullPointerException` abgebildet (Fangen und Weiterwerfen als `NullReferenceException` im generierten `catch`) |
| `IndexOutOfRangeException` | aus `ArrayIndexOutOfBoundsException` abgebildet |
| `InvalidOperationException` | gleicher Name |
| `KeyNotFoundException` | gleicher Name |

`catch`-Blöcke im generierten Java fangen den passenden Java-Typ und die gemappte Klasse. Details legt der Generator-Test fest.

## Namenskonvention (gilt für `runtime-system` und Adapter)

- Typen behalten den Namen, Methoden und Felder werden `lowerCamelCase`.
- Properties → Methode `name()` und `setName(v)`, `bool`-Properties behalten ihren Namen (`isActive()` nur, wenn das Original `IsActive` heißt: dann `isActive()`).
- Indexer → `get(i)` und `set(i, v)`.
- Operatoren → benannte statische Methoden: `op_Addition` → `add`, `op_Subtraction` → `sub`, `op_Multiply` → `mul`, `op_Division` → `div`, `op_Equality` → `equals`, `op_Inequality` → `notEquals`, `op_UnaryNegation` → `neg`.
- Events → `addName(handler)` und `removeName(handler)`.
- Statische Klassen → `final` Klasse mit privatem Konstruktor.
- Konflikte mit Java-Schlüsselwörtern (`default`, `new`, `class` als Methodenname) → Suffix `_` (`default_`).

## Mapping-Datei

Jedes Modul, das Typen bereitstellt (`runtime-system`, jeder Adapter), liefert `mapping.tsv` mit Spalten `IR-Symbol<TAB>Java-Symbol<TAB>Status`. Status: `full`, `stub`. Fehlt ein Symbol, gilt es als `missing` (Lint SH001). Die Datei ist sortiert nach IR-Symbol und deterministisch.

## Beispiel

```
System.Collections.Generic.List<T>::Add#T          io.github.localsoul.runtime.system.collections.generic.List#add(T)         full
System.Collections.Generic.List<T>::get_Count#     io.github.localsoul.runtime.system.collections.generic.List#count()        full
UnityEngine.Vector3::op_Multiply#UnityEngine.Vector3,f32   io.github.localsoul.adapter.unity.Vector3#mul(Vector3,float)        full
```
