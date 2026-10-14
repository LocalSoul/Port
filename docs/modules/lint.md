# Modul `lint`

## Zweck
Prüft die IR eines Projekts mit den Lint-Regeln aus `docs/cli-contract.md` (Kurzreferenz) und liefert Befunde für `port lint`. Grundlage für Agent-Arbeitspakete (M6a).

## Abhängigkeiten
Nur `ir` (Maven Enforcer, ArchUnit). Durchlauf über `ir.refs.NodeWalker` (derselbe Durchlauf wie Referenzen und Validator), Deklarationen über `ir.refs.Declarations`, Hashes über `ir.refs.SymbolSource`.

## API
```java
LintInput input = new LintInput(sources, overrides, coverage);   // IR-Dateien, // port:override, Abdeckung
List<LintFinding> findings = Linter.lint(input, new LintConfig(disable, severity));
```
- `IrSource`: Pfad, Text, gelesene Datei oder Syntaxfehler (IR001).
- `ManualOverride.parse(pfad, javaText)`: Markierungen nach ADR-027.
- `Coverage`: Symbol-ID oder Typname → `full`/`stub`; Typzeilen mit Stelligkeit (`System.Func/2`) zählen für den Typ; ein Stub bleibt Stub.
- `LintConfig`: `[lint] disable` und `[lint.severity]` aus `port.toml`.

## Regeln (Stand M6)

| ID | Stufe | Erkennung |
|---|---|---|
| IR001–IR005 | error/info | IR-Validator; externe Typen gelten ihm als bekannt (ob es sie gibt, prüft SH001) |
| PT001 | error | Referenz auf `System.Reflection.*`, `System.Type::Get{Method,Field,Property,Member,Type}…`, `System.Type::InvokeMember…`, `System.Activator::CreateInstance…` |
| PT002 | error | Typ `ptr` (Ausnahme für nativen Code mit M10b) |
| PT003 | error | `yield-return`/`yield-break` im geschützten Block oder `catch` eines `try` mit `finally` oder in einem Lambda |
| SM001 | warn | Wert eines eigenen `struct`-Typs aus Variable, Parameter oder Feld in `local`, `set` oder `return` (ohne `(copy …)`); ADR-009 OPEN |
| SH001 | error | externer Typ oder externes Member fehlt in der Abdeckung (ein Befund je Quelle und Ziel; `System.Array::Length` ausgenommen) |
| SH002 | warn | externes Symbol nur als Stub |
| MN001 | error | Markierung auf ein Symbol, das es in der IR nicht mehr gibt |
| MN002 | error | Hash der Markierung passt nicht zur aktuellen S-Expression (Präfix, mindestens 12 Zeichen) |
| MN003 | warn | Handarbeitsstelle (`:manual` am Typ/Member, `raw`/`manual` im Rumpf) ohne Markierung |
| MN004 | info | Markierung auf ein Symbol ohne Handarbeit |

PT001–PT003 gelten nicht in Handarbeitsstellen (die meldet MN003). Befunde sind nach Ort, Regel und Meldung sortiert.

## Override-Markierung (ADR-027)
```java
// port:override UnsafeBits::First#i32[] ir=999f30aa4cb9
@Override
public int first(int[] values) { ... }
```
Der Hash ist SHA-256 der S-Expression des Symbols (`port show <symbol>` → `irHash`; `port lint --fix-hint` nennt den fertigen Kommentar bei MN003).

## Abdeckung (Nutzerentscheidung M6)
`port-cli` baut sie aus `adapter-metadata.json` des Adapter-Moduls (Stubs) und allen Einträgen in `mapping.tsv` von Adapter und `runtime-system` (als `full`).

## Tests
Je Regel Treffer und Nicht-Treffer an kleinen IR-Ausschnitten, Konfiguration, Lauf über die Fixtures mit den echten Mappings (nur SH und MN003), ArchUnit.

## Nicht-Ziele
PT004–PT007, SM002–SM009, SH003, FX*, NT* (spätere Meilensteine), Schreiben der Markierung (M7/M7a).
