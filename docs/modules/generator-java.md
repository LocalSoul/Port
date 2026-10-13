# Modul `generator-java`

## Zweck
Erzeugt aus der IR Java-Quelltext. Weiß nichts über die konkrete Engine, nur über das Mapping eines Adapters.

## Abhängigkeiten
Nur `ir` (Maven Enforcer, ArchUnit: nichts aus `adapter-*`, `engine-*`, keine Reflection). Das Mapping eines Adapters wird zur Laufzeit als Datei (`mapping.tsv`) gelesen, nie als Codeabhängigkeit. google-java-format nur im Test-Scope.

## API (Stand M3a)
```java
Mapping mapping = Mapping.load(adapterTsv).merge(Mapping.load(runtimeTsv));  // Adapter + runtime-system
GeneratorConfig config = new GeneratorConfig("gen.mygame", mapping, Set.of("UnsafeBits"));  // Klassen mit manual/
GenerationResult result = new JavaGenerator(config).generate(irFiles);  // List<GeneratedFile>, List<Todo>, sortiert
```

## `mapping.tsv`
Drei Spalten, getrennt durch Tabulatoren: IR-Symbol (stabile Symbol-ID nach ADR-025, also ohne Typargumente am Typ), Java-Symbol, Form. Zeilen mit `#` und Leerzeilen werden übersprungen, doppelte Symbole sind ein Fehler (`MappingException` mit Zeilennummer, ebenso ein Symbol in beiden Mappings bei `merge`).

**Stelligkeit:** Typzeilen dürfen `Name/n` heißen (`System.Func/2`), weil `Func<T>` und `Func<T1,T2>` in der IR gleich heißen. Für einen Typ mit `n` Typargumenten gilt zuerst `Name/n`, dann `Name`. Memberzeilen tragen keine Stelligkeit (`System.Func::Invoke#T`).

| Form | Java-Symbol | Erzeugt |
|---|---|---|
| `type` | voll qualifizierter Klassenname | Import, einfacher Name |
| `method` | Methodenname | `ziel.name(args)` |
| `static-method` | Methodenname | `Typ.name(args)` |
| `getter` | Methodenname | `ziel.name()` (Property oder Feld) |
| `static-getter` | Methodenname | `Typ.name()` |
| `constructor` | Klassenname | `new Typ(args)` |
| `operator` | Methodenname | `Typ.name(a, b)` |
| `helper` | Methodenname | Instanzmethode als statische Hilfe: `Typ.name(ziel, args)` (z. B. `Strings.substring(s, 1)`, `Convert.toString(x)`); `Typ` ist die Typzeile des deklarierenden Typs |
| `message` | Methodenname | gleich signierte Methode einer Unterklasse wird `@Override protected` (Lifecycle, Nutzerentscheidung M3) |

## Ausgabe
- Je IR-Klasse **`FooBase.java`** (abstrakt, alle erzeugten Member) und ein **dünnes `Foo.java`** (`public class Foo extends FooBase`, wiederholt die Konstruktoren). `Foo` entfällt, wenn `manual/Foo.java` existiert (`GeneratorConfig.manualClasses`) oder die Klasse Handarbeit braucht (Nutzerentscheidung M3). Referenzen anderer Klassen zeigen immer auf `Foo`.
- Pakete: Zielpaket plus IR-Namespace in Kleinbuchstaben (`Game.Logic.E` → `<ziel>.game.logic.EBase`).
- Member in kanonischer Reihenfolge (Felder, Properties, Events, Konstruktoren, Methoden, je nach Symbol-ID), Importe sortiert, eine Leerzeile zwischen Membern.

## Generierungsregeln
- **Namen:** Typen behalten den Namen, Felder auch; Methoden und Properties `lowerCamelCase` (`Update` → `update`); Java-Schlüsselwörter bekommen `_` (`new` → `new_`).
- **Properties:** Getter `name()`, Setter `name(T value)`. Zuweisungen an Properties nur als Statement: `this.p(3)`; `p += 2` → `this.p(this.p() + 2)` (nur bei einfachem Ziel ohne Seiteneffekte).
- **Modifier:** C#-`private` → paketsichtbar (damit `manual/Foo` im selben Paket zugreifen kann), `internal` → `public` (C#-Namespaces einer Assembly sehen einander, Java-Pakete nicht), `protected` → `protected`, `readonly` → `final`, `const` → `static final`, `sealed` (Klasse) → `final` am dünnen `Foo`, `override` → `@Override`.
- **Typen:** Primitive laut `docs/type-mapping.md` (`string` → `String`, `object` → `Object`); Primitive als Typargument geboxt (`Box<int>` → `Box<Integer>`); eigene generische Klassen als Java-Generics, `new Box<>(...)`; Arrays mit Rang 1.
- **Literale:** `i8`/`i16` mit Cast (`(byte) -5`), `i64` mit `L`, `f32` mit `f`, `f64` ohne Punkt mit `d`; `char` als `'a'` bzw. `(char) N`; Strings mit Oktal-Escapes für Steuerzeichen (ein `\u000A` würde javac vor dem Lexen umwandeln).
- **Ausdrücke:** Klammern nur, wo die Java-Rangfolge es verlangt; `System.Array::Length` → `array.length`; `unchecked` entfällt (Java rechnet wie C# unchecked); `default(T)` → Nullwert des Typs.
- **Statements:** Blöcke immer mit Klammern; `switch` klassisch mit `case x:` (IR garantiert kein Durchfallen); `foreach` → Java-for-each (Arrays und `Iterable` aus `runtime-system`); `lock` → `synchronized`; `throw` → `throw`.
- **`try`/`catch`/`finally`:** direkt nach Java. Fängt die IR `System.Exception`/`System.SystemException`, `System.NullReferenceException` oder `System.IndexOutOfRangeException`, fängt Java zusätzlich `NullPointerException` bzw. `ArrayIndexOutOfBoundsException` und übersetzt mit `Typ.fromJava(e$)` (Details `docs/type-mapping.md`, Abschnitt Exceptions).
- **Konstruktoren:** `protected FooBase(...)`, `:base-call` → `super(...)`, statischer Konstruktor → `static {}`. Hinweis: In C# laufen Feldinitialisierer vor dem Basiskonstruktor, in Java danach.
- **Lifecycle:** Methoden, deren Signatur (`Update#`) einer `message`-Zeile eines Basistyps entspricht (Basiskette über eigene Klassen bis zum ersten externen Typ), werden `@Override protected` mit dem Java-Namen aus dem Mapping.

## Offene Stellen
- **Todo:** Ein Member mit nicht erzeugbarem Konstrukt bekommt den Rumpf `throw new UnsupportedOperationException("PORT-TODO: <Symbol>: <Grund>")` und einen `Todo`-Eintrag; Importe aus dem abgebrochenen Versuch werden zurückgenommen. Ein Feld-Initialisierer, der sich nicht erzeugen lässt, entfällt (Todo). Andere Typarten als Klassen (struct, interface, enum) werden in M3 nicht erzeugt (Todo).
- **Handarbeit:** `raw`/`manual` im Rumpf → abstrakte Methode in `FooBase`, kein dünnes `Foo`, Todo „Klasse braucht Handarbeit: manual/Foo.java extends FooBase“.
- Todo (Stand M3a) u. a.: externe Symbole ohne Mapping, Unsigned-Typen (ADR-019), Nullable, `ref`/`out`, mehrdimensionale Arrays, Coroutines (ADR-008), Events, Lambdas, `as`, `??`, `typeof`, `checked`, `throw;` (erneutes Werfen, schon im Frontend), `goto`, eigene structs (ADR-009), Interfaces und Enums.

## Formatierung
Eigener Emitter (`JavaWriter`, `ExprEmitter`, `StmtEmitter`, `ClassEmitter`) im google-java-format-Stil, Nutzerentscheidung M3. `LineBreaker` setzt das Layout-Modell `Doc` (Text, Aufruf, Kette) nach den Regeln von google-java-format um: Breite 100, Zuweisungen und Deklarationen brechen zuerst nach `=` (Wert mit +4), Ketten brechen vor `.` (erstes Glied bleibt am einfachen Empfänger), Aufrufe nach `(` mit +4, Argumente zusammen oder je eine Zeile, lange Mehrfach-`catch` an `|`, Javadoc dreizeilig, leere Rümpfe `{}`. Nicht abgedeckt: Umbrüche an binären Operatoren und in anderen Block-Köpfen. `JavaGeneratorGoldenTest` lässt google-java-format über alle Golden Files laufen und verlangt keine Änderung.

## Tests
- Golden Files `testdata/golden/java/` (Java je Fixture-Klasse plus `todos.tsv`) aus `testdata/golden/ir/unity/`, aktualisieren mit `-Dgolden.update=true`.
- Golden-Tests laden die Mappings von `adapter-unity` und `runtime-system`.
- Einzelregeln (`JavaGeneratorTest`): Rangfolge, Literale, Unsigned → Todo, `message` → `@Override`, Konstruktoren im dünnen `Foo`, `manualClasses`, `raw` → abstrakt, Setter, Pakete und Schlüsselwörter, ungültiges Mapping, Importe nach Todo, Stelligkeit, `helper`, `try`/`catch`/`finally` (mit google-java-format geprüft), Umbruch langer Mehrfach-`catch`.
- Kompilier- und Lauftests im Modul `integration-tests` (`MoverEndToEndTest`, `RuntimeSystemEndToEndTest`, StarCollector-Bericht `StarCollectorGeneratorReportTest`).

## Nativer Dialekt (ab M10b)
`cfg`-Funktionen werden als Zustandsautomat erzeugt (`switch` über das aktuelle Label in einer Schleife), `load`/`store` über `NativeMemory`, Ganzzahloperationen mit Zweierkomplement-Wrapping und vorzeichenloser Semantik nach Operandentyp (ADR-019, Unsigned-Teilfrage muss vorher entschieden sein). Siehe `docs/ir-spec.md`, Abschnitt 12.

## Nicht-Ziele
- Keine Optimierungen, keine Umstrukturierung der Logik.
- Kein Wissen über Engine-Interna.
