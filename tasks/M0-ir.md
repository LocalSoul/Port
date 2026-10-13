# M0: Repo-Skelett und IR

**Ziel:** Ein baubares Maven-Multi-Module-Projekt und ein vollständiger, getesteter IR-Parser samt Printer und Validator nach `docs/ir-spec.md`.

**Eingaben:** `AGENTS.md`, `docs/ir-spec.md`, `docs/modules/ir.md`, `docs/conventions.md`, `docs/decisions.md` (ADR-001, ADR-004).

**Abhängigkeiten/Entscheidungen:** ADR-004 (handgeschriebener Reader) ist `PROPOSED`. Du darfst ihm folgen, weise im Bericht darauf hin.

---

## M0.1 Repo-Skelett

**Schritte**
1. Maven-Wrapper, Parent-POM (Java 25, Encoding UTF-8, Plugin-Versionen gepinnt).
2. Module anlegen: `ir` (jetzt), die übrigen noch nicht.
3. Spotless (google-java-format), Maven Enforcer (Java-/Maven-Version, Banned Dependencies), JUnit 5, ArchUnit als Testabhängigkeit.
4. `.gitignore` mit `input/`, `gen/`, `.port/`, `target/`, IDE-Dateien.
5. `./mvnw -q verify` läuft grün (mit einem Platzhaltertest).

**Abnahme**
- [ ] `./mvnw -q verify` ist grün auf einem frischen Checkout.
- [ ] `./mvnw -q spotless:check` ist grün.
- [ ] `.gitignore` schließt `input/`, `gen/`, `.port/` aus.

## M0.2 Lexer und S-Expression-Reader

**Schritte**
1. Lexer nach Spec Abschnitt 2: Klammern, Strings mit Escapes, Zahlen **mit Pflichtsuffix**, Keywords, Atome, Kommentare (`;`).
2. Reader baut daraus eine Listen-/Atom-Struktur mit `SourceSpan` (Datei, Zeile, Spalte) an jedem Knoten.
3. Fehler: eigene `IrSyntaxException` mit Position (`datei:zeile:spalte`) und klarer Meldung.

**Abnahme**
- [ ] Zahl ohne Typsuffix wird mit Position abgelehnt (`5.0` ungültig, `5.0f32` gültig).
- [ ] String-Escapes `\\ \" \n \r \t \uXXXX` funktionieren, ungültige Escapes werden abgelehnt.
- [ ] Nicht geschlossene Klammer und nicht geschlossener String melden die Position der Öffnung.
- [ ] Kommentare werden ignoriert, Positionen bleiben korrekt (Zeilen nach Kommentaren stimmen).

## M0.3 Typ- und Member-Atome

**Schritte**
1. `TypeRef`: Primitive, qualifizierte Namen, Generics `<...>`, Array-Suffixe `[]`, `[,]`, Nullable `?`, `&`.
2. `MemberRef`: `Typ::name`, optional Generics, optional `#` Parameterliste, `.ctor`/`.cctor`.
3. Beide mit Parser (Untersyntax) und Printer. Rückgabe von `toString` ist kanonisch und stabil (= stabile Symbol-ID).

**Abnahme**
- [ ] `Dictionary<string,i32[]>`, `f32[,]`, `System.Collections.Generic.List<i32>` parsen und drucken identisch.
- [ ] `UnityEngine.Transform::Translate#UnityEngine.Vector3`, `Foo::.ctor#f32,f32,f32`, `Foo::Bar#` (leere Parameterliste) parsen und drucken identisch.
- [ ] Fehlerfälle (`Foo::`, `<>`, `i32[`) melden Position und Ursache.

## M0.4 AST und Reader für Deklarationen, Statements, Ausdrücke

**Schritte**
1. AST-Modell als sealed Interfaces und Records, benannt nach IR-Schlüsselwörtern (siehe `docs/modules/ir.md`).
2. Reader wandelt die S-Expression-Struktur in den AST. Alle Knoten der Spec Abschnitte 5 bis 7 und 9.
3. Unbekannte **Knotennamen** werden abgelehnt, unbekannte **Keywords** ignoriert (Spec Abschnitt 11).
4. Attribute `:loc`, `:manual`, `:note` an jedem Knoten.

**Abnahme**
- [ ] Das Beispiel aus Spec Abschnitt 10 wird fehlerfrei gelesen, jeder Knoten hat die erwartete Position.
- [ ] Je Knotenart mindestens ein Positiv- und ein Negativtest (falsche Stelligkeit, falscher Typ des Arguments).
- [ ] Unbekannter Knotenname führt zu Fehler, unbekanntes Keyword wird ignoriert.

## M0.5 Printer (kanonisch)

**Schritte**
1. Printer erzeugt Spec Abschnitt 8: Mitglieder nach stabiler ID sortiert, Reihenfolge Felder → Properties → Konstruktoren → Methoden, Keyword-Reihenfolge fest, 2 Leerzeichen, jeder Kindknoten einer mehrzeiligen Liste auf eigener Zeile, genau ein abschließendes `\n`.
2. Kurze Listen dürfen in einer Zeile stehen. Lege die Regel (Zeilenlänge/Knotenzahl) fest und dokumentiere sie als Kommentar im Printer und in `docs/modules/ir.md`.

**Abnahme**
- [ ] Idempotenz: `print(read(print(read(x)))) == print(read(x))` für alle Fixtures.
- [ ] Strukturelle Gleichheit: `read(print(f))` ist gleich `f`.
- [ ] Zwei Eingaben mit nur unterschiedlicher Mitgliederreihenfolge erzeugen identische Ausgabe.
- [ ] Golden-File-Test für das Beispiel aus Spec Abschnitt 10.

## M0.6 Validator

**Schritte**
1. `IrValidator` erzeugt `Diagnostic`-Objekte mit ID, Schweregrad, Symbol, Meldung, Position.
2. Regeln: **IR002** hängende Referenz (nur innerhalb der Datei prüfbar; externe Typen gelten als aufgelöst, wenn ein Typkatalog übergeben wird, sonst als `info` markiert), **IR003** Typ `unknown`, **IR004** doppelte Symbol-ID, **IR005** Vererbungszyklus. IR001 ist der Syntaxfehler aus dem Reader.
3. Der Validator bricht nicht beim ersten Fehler ab, er sammelt alle.

**Abnahme**
- [ ] Je Regel ein Test mit erwarteter ID, Symbol und Position.
- [ ] Mehrere Fehler in einer Datei werden alle gemeldet, sortiert nach Position, dann ID.
- [ ] Gültiges Beispiel erzeugt keine Diagnosen.

## M0.7 Abschluss

**Abnahme**
- [ ] ArchUnit-Test: `ir` importiert nichts aus anderen Projektmodulen.
- [ ] Javadoc für alle öffentlichen Typen.
- [ ] `docs/modules/ir.md` entspricht dem Stand der Implementierung.
- [ ] `PROGRESS.md` aktualisiert, ADR-004 Status im Bericht erwähnt.

## Nicht-Ziele
- Kein Index, keine Lint-Regeln außer IR001–IR005, keine Java-Generierung, kein Frontend.
