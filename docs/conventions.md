# Konventionen

## Sprache und Werkzeuge

- Java 25, Maven Multi-Module mit Wrapper (`./mvnw`), JUnit 5.
- Formatierung: Spotless mit google-java-format. `./mvnw -q spotless:apply` vor jedem Commit.
- Statische Prüfung: Error Prone oder SpotBugs im Build, Maven Enforcer für Abhängigkeitsregeln, ArchUnit für Paketregeln.
- Basis-Paket: `io.github.localsoul.port`, Gruppen-ID `io.github.localsoul`.
- .NET-Teil (`frontend-unity/dotnet`): .NET 8 LTS oder neuer, `dotnet test`.

## Stil: klassisch und debugbar

Der Nutzer legt Wert auf klassisches Debugging und gute IDE-Unterstützung. Deshalb:

- **Imperativ statt funktional.** Schleifen, lokale Variablen, frühe Returns. Streams nur für triviale Fälle (eine Operation, keine Verschachtelung).
- **Keine Lambda-Ketten** in Kernlogik (Parser, Validator, Generator, Scheduler).
- **Kein Lombok**, keine Annotation-Prozessor-Magie außer Error Prone und JavaPoet-Generierung.
- **Keine Reflection** im Produktionscode, außer wo ein Adapter sie ausdrücklich braucht und dokumentiert.
- **Zwischenvariablen sind erwünscht**, damit sich Zwischenwerte im Debugger ansehen lassen.
- Methoden kurz halten (Richtwert: unter 40 Zeilen), eine Aufgabe pro Methode.
- Records und sealed Interfaces nur für reine Datenmodelle (IR-Knoten, DTOs, Diagnosen).
- Pattern-Matching `switch` über sealed Typen ist erlaubt und erwünscht, wenn es Besucher-Boilerplate spart.

## Namen

- Klassen `UpperCamelCase`, Methoden/Felder `lowerCamelCase`, Konstanten `UPPER_SNAKE_CASE`.
- IR-Knotenklassen heißen wie das IR-Schlüsselwort: `(if ...)` → `IfStmt`, `(scall ...)` → `StaticCallExpr`.
- Testklassen `<Klasse>Test`, Testmethoden beschreiben Verhalten: `rejectsUnknownNodeName`.

## Fehlerbehandlung

- Eigene Exception-Typen pro Modul, die von einer gemeinsamen Basis erben (`PortException` in `port-cli`, `IrException` in `ir`).
- **Parserfehler tragen Position** (`datei:zeile:spalte`) und eine klare Meldung. Nie nur „Syntax error".
- Keine verschluckten Exceptions. Keine `catch (Exception e)` ohne Weiterwurf oder dokumentierten Grund.
- Domänenfehler sind Diagnosen (siehe `docs/cli-contract.md`), keine Exceptions, wenn der Lauf weitergehen kann (Lint, Validator).

## Logging und Ausgabe

- Bibliotheksmodule schreiben **nie** auf `System.out`/`System.err`. Sie liefern Rückgabewerte oder Diagnosen.
- Nur `port-cli` gibt aus. Menschenlesbar auf stdout/stderr, maschinenlesbar mit `--json` (siehe CLI-Vertrag).
- Logging über `java.lang.System.Logger`, keine zusätzliche Logging-Bibliothek ohne ADR.

## Determinismus

- Ausgaben nie aus `HashMap`/`HashSet` iterieren. Nutze `LinkedHashMap`, `TreeMap` oder sortiere explizit.
- Keine Zeitstempel, Zufallszahlen, absoluten Pfade oder Hostnamen in Artefakten.
- Zeilenende `\n`, Encoding UTF-8, Datei endet mit genau einem `\n`.
- Gleiche Eingabe muss byte-identische Ausgabe erzeugen. Dafür gibt es Tests.

## Tests

- JUnit 5. Jede öffentliche Methode mit Verhalten hat mindestens einen Test, jeder Fehlerfall einen eigenen.
- **Golden-File-Tests** für IR, Generator-Ausgabe und JSON-Ausgaben unter `testdata/golden/`. Aktualisierung nur bewusst mit einem Skript oder Flag (`-Dgolden.update=true`), nie automatisch.
- **Roundtrip-Tests** für die IR: `parse(print(parse(x)))` ist strukturell gleich `parse(x)`, `print` ist idempotent.
- **Kein Zugriff auf Netz oder echte Spiele.** Fixtures sind synthetisch und im Repo erzeugbar (siehe `testdata/`).
- Engine-Tests laufen headless mit deterministischem Fixed-Timestep.
- Ein fehlschlagender Test wird nie gelöscht oder abgeschwächt, um grün zu werden. Entweder der Code oder der Test ist falsch, das stellst du fest und begründest es.

## Dokumentation

- Öffentliche Klassen und Methoden: Javadoc mit Verhalten und Fehlerfällen.
- Änderungen an Verhalten, das in `docs/` beschrieben ist, aktualisieren die Doku im selben Commit.
- Architekturentscheidungen stehen in `docs/decisions.md`, nicht in Code-Kommentaren.

## Commits

- Conventional Commits: `feat(ir): ...`, `fix(engine-core): ...`, `test(...)`, `docs(...)`, `refactor(...)`, `chore(...)`.
- Ein Commit = eine nachvollziehbare Änderung, Tests im selben Commit.
- Commit-Nachrichten auf Deutsch oder Englisch, aber innerhalb eines Branches einheitlich.
- Nicht committen: `input/`, `gen/`, `.port/`, `target/`.
