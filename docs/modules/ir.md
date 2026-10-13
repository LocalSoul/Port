# Modul `ir`

## Zweck
Datenmodell, Parser, Printer und Validator für die IR. Quelle der Wahrheit: `docs/ir-spec.md`.

## Abhängigkeiten
Keine zu anderen Projektmodulen. Nur JDK und Testbibliotheken.

## Öffentliche API (Skizze)
```java
public final class IrReader {
    IrFile read(String text, String fileName) throws IrSyntaxException;
}
public final class IrPrinter {
    String print(IrFile file);               // kanonisch, deterministisch
}
public final class IrValidator {
    List<Diagnostic> validate(IrFile file);  // IR001-IR005, ab M10b auch IR006-IR010
}
public record Diagnostic(String id, Severity severity, String symbol, String message, String location, String hint) {}
```
- Knoten sind sealed Interfaces und Records (`ClassDecl`, `MethodDecl`, `IfStmt`, `StaticCallExpr`, ...), benannt nach dem IR-Schlüsselwort.
- Typ- und Member-Atome haben eigene Wertklassen (`TypeRef`, `MemberRef`) mit Parser für die Untersyntax (`Dictionary<string,i32[]>`, `Ns.Typ::name#i32,string`).
- Jeder Knoten trägt eine Quellposition (`SourceSpan`: Datei, Zeile, Spalte).

## Lexik und S-Expression-Ebene (M0.2, Paket `io.github.localsoul.port.ir.sexpr`)
- `IrLexer` liefert Tokens, `SExprReader.readAll(text, fileName)` baut daraus `SExpr`-Knoten (`SList`, `SSymbol`, `SKeyword`, `SString`, `SNumber`, `SBareInteger`), jeder mit `SourceSpan`.
- Positionen: Zeile und Spalte ab 1, Spalte in Unicode-Codepoints, Tab zählt 1. Nur `\n` beginnt eine neue Zeile, `\r` ist Whitespace.
- Atome werden nur an Whitespace, `(`, `)`, `"` und `;` getrennt. Typ- und Member-Atome (`Dictionary<string,i32[]>`, `Foo::.ctor#f32`) bleiben hier ganz und werden erst in M0.3 zerlegt.
- Zahlen: Ein Atom, das mit einer Ziffer oder mit `-` und einer Ziffer beginnt, ist eine Zahl und braucht ein Typsuffix. Exponent: `e`/`E`, optional `+`/`-`, Ziffern. Zahlen mit Punkt oder Exponent brauchen `f32`/`f64`. Hexzahlen: `0x`, kein Vorzeichen, nur Ganzzahl-Suffixe.
- Ausnahme: Eine nicht-negative Ganzzahl ohne Punkt und Exponent wird als `SBareInteger` geliefert, weil die Spec sie an einigen Stellen ohne Suffix verwendet (`(char 97)`, `:version 1`, `:size 24`). Die AST-Ebene (M0.4) lehnt sie an allen anderen Stellen als „Zahl ohne Typsuffix" ab.
- Strings dürfen Zeilenumbrüche enthalten. Escapes: `\\ \" \n \r \t \uXXXX` (genau 4 Hexziffern), alles andere ist ein Fehler an der Position des Backslashs.
- Fehler: `IrSyntaxException` (Basis `IrException`, geprüft) mit `span()` und `reason()`, Meldung `datei:zeile:spalte: Grund`. Nicht geschlossene Klammern und Strings melden die Position der Öffnung.

## Verhalten
- Der native Dialekt (Spec Abschnitt 12) wird erst in M10b implementiert. Bis dahin lehnt der Reader seine Knoten mit klarer Meldung ab.
- Der Reader lehnt unbekannte **Knotennamen** ab und ignoriert unbekannte **Keywords** (Spec Abschnitt 11).
- Fehler enthalten Position und klare Meldung.
- Der Printer erzeugt die kanonische Reihenfolge (Spec Abschnitt 8): Mitglieder nach stabiler ID sortiert, Felder vor Properties vor Konstruktoren vor Methoden, Keyword-Reihenfolge fest, 2 Leerzeichen Einrückung, genau ein abschließendes `\n`.
- `print(read(x))` ist idempotent und `read(print(f))` ist strukturell gleich `f`.

## Tests
- Beispiel aus Spec Abschnitt 10 als Golden File.
- Roundtrip, Idempotenz, Fehlerfälle mit erwarteter Position.
- Eine Testklasse pro Knotengruppe (Deklarationen, Statements, Ausdrücke, Atome).

## Nicht-Ziele
- Keine Übersetzung nach Java, kein Index, keine Lint-Regeln PT/SM/SH/MN (die kommen später in eigenen Modulen).
