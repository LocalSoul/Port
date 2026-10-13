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
