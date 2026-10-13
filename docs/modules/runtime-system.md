# Modul `runtime-system`

## Zweck
Java-Nachbau des .NET-Basisbibliothek-Ausschnitts (`System.*`), den übersetzter Spielcode benutzt. Der Generator bildet IR-Symbole über `mapping.tsv` auf dieses Modul ab (ADR-019).

## Abhängigkeiten
Keine zu anderen Projektmodulen. Nur JDK.

## Umfang (wächst bedarfsgesteuert)

Minimalumfang in M3a:
- `System.Collections.Generic`: `List<T>`, `Dictionary<K,V>`, `HashSet<T>`, `Queue<T>`, `Stack<T>`
- `System`: `Math` (Grundfunktionen), `String`-Hilfen, `Exception` und die häufigsten Ableitungen (siehe `docs/type-mapping.md`)
- Funktionale Interfaces für `Action`/`Func` (0 bis 4 Parameter)

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
- Konsistenztest: jedes `full`-Symbol der `mapping.tsv` existiert als Methode.
- ArchUnit: kein Import aus anderen Projektmodulen.

## Nicht-Ziele
Vollständiger Nachbau der .NET-Bibliothek, Reflection, `System.IO`, Netzwerk, Threading (außer was ADR-008 später verlangt).
