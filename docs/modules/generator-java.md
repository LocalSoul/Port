# Modul `generator-java`

## Zweck
Erzeugt aus der IR Java-Quelltext. Weiß nichts über die konkrete Engine, nur über das Mapping eines Adapters.

## Abhängigkeiten
`ir`. Zur Laufzeit liest er das Mapping eines Adapters als Datei (`mapping.tsv`), nie als Codeabhängigkeit auf `adapter-*`.

## Eingaben und Ausgaben
- Eingabe: `IrFile`, Mapping-Tabelle, Konfiguration (Ziel-Paket, Adapter-Paketname).
- Ausgabe: pro Klasse `FooBase.java` (generiert), plus eine Liste offener Stellen (`todo`).
- Handarbeit liegt in `manual/` als `Foo extends FooBase`. Der Generator überschreibt `manual/` nie.

## Generierungsregeln (Auszug)
- `UnityEngine.Component::transform` (Property) → `transform()`, Methoden → `lowerCamelCase`, statische Aufrufe → `Typ.methode(...)`.
- Operator-Aufrufe (`scall ...::op_Multiply`) → laut Mapping statische Methoden (`Vector3.mul`).
- `local` → lokale Variable. `set` als Ausdruck → Zuweisung. `post-inc` usw. → Java-Äquivalent.
- Primitive: `i8..i64`, `u8..u64`, `f32`, `f64`, `bool`, `char`, `string`. **Unsigned-Typen:** Die Abbildung hängt an ADR-019 (Teilfrage OPEN). Bis dahin als `todo` erzeugen und melden (Lint SM002). Primitive und Bibliothekstypen laut `docs/type-mapping.md`.
- `switch` ohne Durchfallen, `try/catch/finally`, Schleifen 1:1.
- `yield-return`, `lambda` mit Capture-Sonderfällen, Events: erst nach den zugehörigen ADRs (ADR-008). Bis dahin wird ein `todo`-Block erzeugt (`throw new UnsupportedOperationException("PORT-TODO: <Symbol>")`) und in der Ergebnisliste gemeldet.
- `(raw cs ...)` und `:manual` am Member: Rumpf wird nicht generiert, `abstract`- oder Hook-Methode, die `manual/` implementiert.
- Generierter Code importiert **nur** den gewählten Adapter und `runtime-system` (siehe `docs/type-mapping.md`).

## Technik
- JavaPoet oder ein eigener, einfacher Code-Emitter. Empfehlung: eigener Emitter mit `StringBuilder` und Einrückungsverwaltung, wenn JavaPoet beim Debuggen im Weg steht (klassischer Stil).
- Ausgabe deterministisch, formatiert (google-java-format im Test geprüft).

## Nativer Dialekt (ab M10b)
`cfg`-Funktionen werden als Zustandsautomat erzeugt (`switch` über das aktuelle Label in einer Schleife), `load`/`store` über `NativeMemory`, Ganzzahloperationen mit Zweierkomplement-Wrapping und vorzeichenloser Semantik nach Operandentyp (ADR-019, Unsigned-Teilfrage muss vorher entschieden sein). Siehe `docs/ir-spec.md`, Abschnitt 12.

## Tests
- Golden Files pro IR-Konstrukt (`testdata/golden/java/`).
- Kompilier-Test: generierter Code kompiliert gegen den Adapter (nur im Integrationstest, nicht im Unit-Test des Generators).
- `todo`-Meldungen für nicht unterstützte Konstrukte.

## Nicht-Ziele
- Keine Optimierungen, keine Umstrukturierung der Logik.
- Kein Wissen über Engine-Interna.
