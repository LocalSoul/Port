# Modul `ir`

## Zweck
Datenmodell, Parser, Printer und Validator für die IR. Quelle der Wahrheit: `docs/ir-spec.md`.

## Abhängigkeiten
Keine zu anderen Projektmodulen. Nur JDK und Testbibliotheken.

## Öffentliche API (Stand M0)
```java
// io.github.localsoul.port.ir.reader
public final class IrReader {
    IrFile read(String text, String fileName) throws IrSyntaxException;   // IR001 als Exception
}
// io.github.localsoul.port.ir.printer
public final class IrPrinter {
    String print(IrFile file);               // kanonisch, deterministisch
}
// io.github.localsoul.port.ir.validate
public final class IrValidator {
    IrValidator();                           // ohne Typkatalog
    IrValidator(TypeCatalog catalog);
    List<Diagnostic> validate(IrFile file);  // IR002-IR005, ab M10b auch IR006-IR010
    static Diagnostic syntaxError(IrSyntaxException e);   // IR001
}
public record Diagnostic(String id, Severity severity, String symbol, String message, SourceSpan location, String hint) {}
```

Pakete und erlaubte Abhängigkeiten (per ArchUnit geprüft, `ArchitectureTest`):

| Paket | Inhalt | darf nutzen |
|---|---|---|
| `ir` | `SourceSpan`, `IrException`, `IrSyntaxException`, `Identifiers` | JDK |
| `ir.sexpr` | Lexer, S-Expression-Reader | `ir` |
| `ir.atom` | `TypeRef`, `MemberRef` | `ir` |
| `ir.ast` | AST-Records, `SymbolIds` | `ir`, `ir.atom`, `ir.sexpr` |
| `ir.reader`, `ir.printer`, `ir.validate` | Reader, Printer, Validator | alle oben |

Das ganze Modul nutzt nur das JDK, keine Reflection und schreibt nie auf `System.out`/`System.err`.

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

## Typ- und Member-Atome (M0.3, Paket `io.github.localsoul.port.ir.atom`)
- `TypeRef.parse(text, atomStart)` liefert einen sealed `TypeRef`: `PrimitiveType`, `NamedType` (qualifizierter Name plus Typargumente), `ArrayType` (Rang ≥ 1), `NullableType`, `ByRefType`.
- `MemberRef.parse(text, atomStart)` liefert deklarierenden Typ (qname), dessen Typargumente (`declaringTypeArguments`, ADR-025, z. B. `List<UnityEngine.Transform>::Add#T`), Namen (Bezeichner, `.ctor`, `.cctor`), generische Argumente der Methode und optional die Parameterliste. `hasParameterList` unterscheidet `Foo::Bar#` (Methode ohne Parameter) von `Foo::Bar` (Feld/Property).
- `MemberRef.symbolId()` ist die stabile Symbol-ID: die Referenz ohne Typargumente am deklarierenden Typ (`System.Collections.Generic.List::Add#T`). `toString()` enthält sie.
- Beide sind Wertobjekte **ohne** Quellposition (gleicher Text ergibt gleiche Objekte). Die Position trägt der AST-Knoten. `toString()` ist kanonisch.
- Suffixe gelten von links nach rechts (`i32?[]` = Array von `i32?`). `&` muss das letzte Suffix sein, `??` ist verboten. Primitive haben keine Typargumente, `<>` ist leer und damit ungültig.
- Fehlerpositionen = Position des Atoms plus Versatz der Fehlerstelle. Nicht geschlossene `[` und `<` melden die öffnende Stelle.

## AST und Reader (M0.4, Pakete `io.github.localsoul.port.ir.ast` und `...ir.reader`)
- Einstieg: `new IrReader().read(text, fileName)` liefert `IrFile` (Dateiname, `IrHeader`, Typen in Eingabereihenfolge). Der Reader bricht beim ersten Fehler mit `IrSyntaxException` ab.
- Knoten: sealed Interfaces `IrNode` → `Expr`, `Stmt`, `TypeDecl`, `MemberDecl` plus Teilknoten (`SwitchCase`, `SwitchDefault`, `CatchClause`, `FinallyClause`, `Param`, `AttributeUse`, `EnumMember`, `IrHeader`). `RawNode` und `ManualNode` sind zugleich `Expr` und `Stmt`. Jeder Knoten hat `span()` und `attrs()` (`NodeAttrs`: `:loc`, `:manual`, `:note`).
- Optionale Teile sind `null` (z. B. `IfStmt.elseBranch`, `CtorDecl.baseCallArguments` – `null` = kein `:base-call`, leere Liste = `:base-call ()`). `MethodDecl.body` ist `null` genau bei `:abstract`.
- Keywords: Bekannte Keywords mit Wert nehmen genau das nächste Element, doppelte sind ein Fehler. Flags ohne Wert: `:abstract` (method), `:ref`/`:out`/`:in` (Parameter). **Unbekannte Keywords** werden ignoriert und nehmen das nächste Element als Wert mit, wenn es kein Keyword ist. Künftige Keywords sollten deshalb immer einen Wert haben.
- Positionsargumente stehen in Eingabereihenfolge, Keywords dürfen dazwischen stehen (z. B. `(method M void () :mods (public) (block))`).
- Nativer Dialekt: Knotennamen aus Spec Abschnitt 12 (`cfg`, `bb`, `load`, `store`, `ncall`, `br`, ...) und das Keyword `:native` werden mit „nativer Dialekt nicht unterstützt" abgelehnt. `(raw asm ...)` ist als Teil von Abschnitt 9 erlaubt.
- Zusätzliche Prüfungen des Readers: Wertebereich von Zahlen (dezimal nach Typ, Hex als Bitmuster bis zur Typbreite, Gleitkomma ohne Überlauf auf Unendlich), `nat` nur in `:version`, `(char ...)` (0–65535) und `:size` (ADR-024), `:version` muss 1 sein, Modifier nur aus der Liste in Spec Abschnitt 5, `new` braucht eine `.ctor`-Referenz, Ziele von `set`/`set-op`/`pre-inc` usw. müssen L-Werte sein, `(default ...)` bzw. `(finally ...)` muss der letzte Zweig sein, höchstens eines von `:ref`/`:out`/`:in`.
- `(copy expr)` (Spec Abschnitt 7, Regeln) wird als `CopyExpr` gelesen.
- Fehlermeldungen unterscheiden „unbekannter Knotenname", „ist ein Statement/Ausdruck an dieser Stelle", „falsche Stelligkeit" (mit Kurzsyntax) und „X erwartet, gefunden Y".
- Nicht im Reader geprüft (Validator oder später): `switch`-Zweige ohne Durchfallen, Symbolauflösung (IR002), doppelte IDs (IR004), Zyklen (IR005).

## Printer (M0.5, Paket `io.github.localsoul.port.ir.printer`)
- Einstieg: `new IrPrinter().print(file)`. Zweistufig: AST → Layout-Modell (`Doc`, dabei kanonisch sortiert) → Text (`Layout`).
- **Reihenfolge:** Typen nach ID; Mitglieder nach Gruppe Felder → Properties → **Events** → Konstruktoren → Methoden, innerhalb der Gruppe nach stabiler ID (`SymbolIds`). Events nennt die Spec nicht, sie stehen nach den Properties. Enum-Werte nach Namen. Vergleich ordinal (`String.compareTo`). Alles andere (Parameter, Argumente, `:implements`, `:mods`, `:attrs`) bleibt in Eingabereihenfolge.
- **Keywords:** Reihenfolge der Grammatik, danach `:loc`, `:manual`, `:note` als letzte Elemente des Knotens (auch hinter Mitgliedern oder Rumpf). Leere optionale Listen (`:mods ()`, `:implements ()`, `:generics ()`, `:attrs ()`) entfallen. `:base-call ()` bleibt, weil es sich von fehlendem `:base-call` unterscheidet.
- **Zeilenregel:**
  1. Eine Liste steht in einer Zeile, wenn nichts in ihr einen Umbruch erzwingt und ihre einzeilige Form ab ihrer Startspalte höchstens **100 Zeichen** erreicht. Schließende Klammern umgebender Listen zählen nicht mit.
  2. Einen Umbruch erzwingen: nicht-leere `block`, Typen mit Mitgliedern, Enums mit Werten.
  3. Sonst: erste Zeile `(kopf`, dann der Reihe nach Elemente in derselben Zeile, solange sie einzeilig passen und keinen Umbruch erzwingen. Bei Typen dürfen nur die Typattribute in die erste Zeile, Mitglieder nie. Bricht `block` oder `switch` um, stehen Statements bzw. Zweige immer auf eigenen Zeilen.
  4. Jedes weitere Element steht auf eigener Zeile, 2 Leerzeichen tiefer als die Zeile der öffnenden Klammer. Die schließende Klammer folgt direkt dem letzten Element.
- Datei: Kopf, dann je eine Leerzeile vor jedem Typ, genau ein abschließendes `\n`. Kommentare aus der Eingabe gehen verloren.
- Strings: Kurzformen für `\\ \" \n \r \t`. Andere Steuerzeichen und einzelne Surrogate werden als `\uXXXX` geschrieben, alles andere bleibt unverändert (UTF-8).
- Literale und Bezeichner sind Atome und können keine Attribute tragen. Ein solcher Knoten mit Attributen führt zu `IllegalArgumentException`.
- Golden Files: `testdata/golden/ir/<fixture>.ir` für jede Fixture in `testdata/ir/`. Aktualisieren nur bewusst mit `./mvnw -pl ir test -Dgolden.update=true`.

## Validator (M0.6, Paket `io.github.localsoul.port.ir.validate`)
- Einstieg: `new IrValidator()` oder `new IrValidator(typeCatalog)`, dann `validate(file)`. Das liefert eine unveränderliche, sortierte `List<Diagnostic>`. `IrValidator.syntaxError(e)` wandelt einen Reader-Fehler in IR001 um.
- `Diagnostic(id, severity, symbol, message, location, hint)`: Felder wie im JSON-Vertrag. Abweichend von der Skizze ist `location` ein `SourceSpan` (`toString()` = `datei:zeile:spalte`). `symbol` ist die ID des umgebenden Typs oder Mitglieds, bei IR001 `null`. `hint` ist optional.
- Sortierung `Diagnostic.ORDER`: Datei, Zeile, Spalte (numerisch, Zeile 9 vor 10), dann ID, dann Meldung.
- **IR002:** Typen der Datei und generische Typparameter (Typ und Methode) gelten als aufgelöst. Externe Typen: mit `TypeCatalog` ein Fehler, wenn unbekannt; ohne Katalog **einmal pro Typname** `info` „nicht geprüft". Member-Referenzen auf Typen der Datei müssen dort deklariert sein:
  - ohne `#`: Feld, Property, Event oder Enum-Wert gleichen Namens,
  - `.ctor`/`.cctor`: Konstruktor mit passendem `static` und gleichen Parametertypen,
  - Methode: gleiche Parametertypen; bei generischen Methoden nur Name, Anzahl der Typparameter und Anzahl der Parameter.
  
  Geprüft werden auch Typen in Typargumenten (auch am deklarierenden Typ), Arrays und Member-Signaturen. Attributtypen (`:attrs`) zählen als Typreferenz. Parametertypen sind offen (ADR-025): Bei Referenzen auf generische Typen oder Methoden gelten einfache Namen ohne Punkt (`T`) als Typparameter und werden nicht aufgelöst.
- **IR003:** `unknown` irgendwo in einem Typ (auch verschachtelt), `error`.
- **IR004:** doppelte Typnamen und doppelte Mitglieds-IDs je Typ (Felder, Properties und Events teilen `Typ::name`). Gemeldet wird jedes weitere Vorkommen mit Verweis auf das erste. Parameter mit `:ref`/`:out`/`:in` ergeben andere IDs (`i32&`).
- **IR005:** Zyklus über `:base` und `:implements` zwischen Typen der Datei. Jeder Typ auf dem Zyklus wird gemeldet, mit Pfad (`A -> B -> C -> A`).
- Positionen: Typreferenzen haben keine eigene Position und melden die Position des umgebenden Knotens (Feld, Local, Ausdruck).
- Nicht geprüft: lokale Variablen und Parameter (`IdentExpr`), Sprungmarken, `switch` ohne Durchfallen, Members geerbter externer Typen.

## Verhalten
- Der native Dialekt (Spec Abschnitt 12) wird erst in M10b implementiert. Bis dahin lehnt der Reader seine Knoten mit klarer Meldung ab.
- Der Reader lehnt unbekannte **Knotennamen** ab und ignoriert unbekannte **Keywords** (Spec Abschnitt 11).
- Fehler enthalten Position und klare Meldung.
- Der Printer erzeugt die kanonische Reihenfolge (Spec Abschnitt 8): Mitglieder nach stabiler ID sortiert, Felder vor Properties vor Konstruktoren vor Methoden, Keyword-Reihenfolge fest, 2 Leerzeichen Einrückung, genau ein abschließendes `\n`.
- `print(read(x))` ist idempotent und `read(print(f))` ist strukturell gleich `f`.

## Tests
- Beispiel aus Spec Abschnitt 10 als Golden File (`testdata/ir/spec-example.ir`).
- Roundtrip, Idempotenz, Fehlerfälle mit erwarteter Position.
- Eine Testklasse pro Knotengruppe (Deklarationen, Statements, Ausdrücke, Atome).

## Nicht-Ziele
- Keine Übersetzung nach Java, kein Index, keine Lint-Regeln PT/SM/SH/MN (die kommen später in eigenen Modulen).
