# M3a: `runtime-system` (BCL-Nachbau) und Typabbildung

**Ziel:** Übersetzter Spielcode kann den häufigsten Teil der .NET-Basisbibliothek nutzen (`List<T>`, `Dictionary<K,V>`, Strings, Math, Exceptions, `Func`/`Action`), mit .NET-Semantik und Tests der Unterschiede. Der Generator nutzt das `mapping.tsv` von `runtime-system`; eine Fixture mit Collections läuft End-to-End.

**Eingaben:** `AGENTS.md`, `docs/modules/runtime-system.md`, `docs/type-mapping.md`, `docs/modules/generator-java.md`, `docs/decisions.md` (ADR-019, ADR-025).

**Abhängigkeiten/Entscheidungen:** ADR-019 ACCEPTED mit Detailentscheidungen (2026-10-13): Dictionary-Reihenfolge wie .NET, Zahlformat wie Unity-Mono, eigene Exception-Hierarchie. Unsigned-Typen bleiben `todo` (Teilfrage OPEN). Lambdas sind kein Ziel (Frontend erzeugt sie noch nicht).

---

## M3a.1 Modul `runtime-system`

**Schritte**
1. Modul `runtime-system` (nur JDK), Paket `io.github.localsoul.runtime.system`, Unterpakete spiegeln .NET.
2. `List<T>`, `Dictionary<K,V>`, `HashSet<T>`, `Queue<T>`, `Stack<T>`, `KeyValuePair<K,V>`; Strings, `Convert`, `Math`, `MathF`; Exceptions; `Func0`–`Func4`, `Action0`–`Action4`.
3. `mapping.tsv` für alle umgesetzten Symbole.

**Abnahme**
- [x] Verhaltenstests gegen dokumentiertes .NET-Verhalten: Indexer und `RemoveAt` außerhalb des Bereichs, Änderung während `foreach`, doppelter/fehlender Schlüssel, Dictionary-Reihenfolge nach `Remove`/`Add`, Banker's Rounding, Zahlformat (G7/G15), String-Verkettung mit `null`.
- [x] Konsistenztest: jedes Mapping-Ziel existiert.
- [x] ArchUnit: nur JDK, keine Reflection, keine Projektmodule.

## M3a.2 Generator

**Schritte**
1. Typzeilen im Mapping mit Stelligkeit (`System.Func/1`).
2. Adapter- und Runtime-Mapping zusammen.
3. `try`/`catch`/`finally`, `throw`, Delegate-Aufrufe, String-Operatoren, `foreach` über `Iterable`.

**Abnahme**
- [x] Golden Files aktualisiert, keine BCL-Todos mehr für die Fixtures (außer Lambdas und Unsigned), google-java-format-konform.
- [ ] `catch` von `System.Exception`/`NullReferenceException` fängt auch Java-NPE (Test).

## M3a.3 End-to-End

**Abnahme**
- [ ] `Inventory` (List, Indexer, Func, eigener generischer Typ) und `ControlFlow.Score` laufen mit erwarteten Ergebnissen.
- [ ] Neue Fixture mit `Dictionary` (Reihenfolge nach Remove/Add) und `try`/`catch` läuft C# → IR → Java mit Erwartungswerten aus .NET-Semantik.
- [ ] StarCollector-Todo-Bericht ohne BCL-Lücken.

## M3a.4 Abschluss
- [ ] Doku `runtime-system.md`, `type-mapping.md`, `generator-java.md` aktualisiert.
- [ ] Lücken und offene Prüfung des Zahlformats gegen echtes Unity in `PROGRESS.md`.

## Nicht-Ziele
Unsigned-Typen, Lambdas/Closures, LINQ, `System.IO`, Threading, Reflection, Kulturabhängige Formatierung.
