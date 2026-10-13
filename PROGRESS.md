# PROGRESS.md

Wird von Agents am Ende jeder Session aktualisiert. Kurz und konkret halten.

## Aktueller Stand

- Phase: **M0 abgeschlossen** (alle Abnahmekriterien in `tasks/M0-ir.md` erfüllt, `./mvnw -q verify` grün, 465 Tests)
- Branch `task/M0-skeleton`, noch nicht nach `main` gemergt
- Nächste Aufgabe: `tasks/M1-engine-core.md` (neuer Branch `task/M1-...`), vorher die offenen Punkte unten durchgehen
- Letzte Session: 2026-10-13, M0.2 bis M0.7 und ADR-024

## Erledigt

- Setup: Git-Repo (`main`), Agent-Kit committet, `dev.example` → `io.github.localsoul`, `.gitignore` (inkl. `original/`). Ordner `original/` (Papers, Please) auf Wunsch des Nutzers gelöscht.
- M0.1 Repo-Skelett: Parent-POM `io.github.localsoul:recomp-parent` (Java 25, UTF-8, alle Plugins gepinnt, `project.build.outputTimestamp` fest), Modul `ir`, Maven-Wrapper 3.3.4 (only-script, Maven 3.9.16), Spotless (google-java-format 1.36.1, `check` in `verify`), Enforcer (Java ≥ 25, Maven ≥ 3.9, gepinnte Plugins, Dependency-Konvergenz, Verbot von Lombok/JUnit 4/Logging-Bibliotheken; in `ir` zusätzlich keine Projektmodule), JUnit 5.14.4, ArchUnit 1.5.0 (Test-Scope), Platzhaltertest.
- M0.2 Lexer und S-Expression-Reader (Paket `io.github.localsoul.port.ir.sexpr`): `IrLexer`, `SExprReader`, `SExpr`-Knoten mit `SourceSpan`, `IrException`/`IrSyntaxException`. 73 Tests. Folgt ADR-004 (handgeschriebener Reader, Status weiter `PROPOSED`). Platzhaltertest entfernt.
- M0.3 Typ- und Member-Atome (Paket `io.github.localsoul.port.ir.atom`): `TypeRef` (sealed: Primitive, benannt/generisch, Array, Nullable, ByRef), `MemberRef` (stabile Symbol-ID), Parser mit Fehlerposition im Atom. Insgesamt 134 Tests.
- ADR-024 (`nat` ohne Suffix in `:version`, `(char ...)`, `:size`) vom Nutzer freigegeben, Spec angepasst. `docs/ir-spec.md` ist schreibgeschützt (vom Nutzer so gewollt), für die Änderung wurde der Schutz mit Erlaubnis kurz aufgehoben und wiederhergestellt.
- M0.4 AST und Reader: Paket `ast` (sealed `IrNode`/`Expr`/`Stmt`/`TypeDecl`/`MemberDecl`, Records je Knoten), Paket `reader` (`IrReader`). Spec-Beispiel liegt als Fixture in `testdata/ir/spec-example.ir`. Insgesamt 419 Tests.
- M0.5 Printer (Paket `printer`): kanonische Sortierung, feste Keyword-Reihenfolge, Zeilenregel (100 Zeichen, 2 Leerzeichen) in `docs/modules/ir.md`. `SymbolIds` für Deklarationen. Golden Files in `testdata/golden/ir/` (Update nur mit `-Dgolden.update=true`), zweite Fixture `testdata/ir/all-nodes.ir` mit jeder Knotenart. Strukturvergleich ohne Positionen über Test-Helfer `AstAssertions` (Record-Reflection nur im Testcode).
- M0.6 Validator (Paket `validate`): IR002–IR005, IR001 aus Reader-Fehlern, `TypeCatalog` für externe Typen, Sortierung nach Datei/Zeile/Spalte (numerisch)/ID.
- M0.7 Abschluss: ArchUnit-Tests (nur JDK, keine Reflection, keine Konsolenausgabe, Schichtung der Pakete), Doclint ohne Warnungen (`javadoc -Xdoclint:all`, nicht im Build), `docs/modules/ir.md` auf Stand. **ADR-004** (handgeschriebener Reader) wurde gefolgt, Status weiterhin `PROPOSED`, bitte freigeben oder verwerfen.

## Offene Punkte / Nebenfunde

- .NET SDK defekt/unvollständig: `dotnet --version` meldet `/usr/lib64/dotnet/host/fxr does not contain any version-numbered child folders`. Vor M2 beheben.
- Zielspiel wird ein selbst kompiliertes 2048 (Unity, möglichst Mono-Build, siehe ADR-003). Noch nicht vorhanden.
- Statische Prüfung (Error Prone oder SpotBugs, `docs/conventions.md`) ist noch nicht im Build. Kein Abnahmekriterium von M0, Wahl durch den Nutzer offen. Doclint könnte bei der Gelegenheit ebenfalls in den Build.
- Die SLF4J-Warnung „No SLF4J providers were found“ beim Build stammt von Werkzeugen im Build und ist harmlos.
- Spec-Auslegung in M0.2, bitte bestätigen (Details in `docs/modules/ir.md`):
  - Ganzzahlen ohne Suffix: entschieden in ADR-024 (`nat` nur in `:version`, `(char ...)`, `:size`), Spec angepasst.
  - Die Spec definiert `exp` nicht. Umgesetzt als `e`/`E`, optional `+`/`-`, Ziffern.
  - Ganzzahl-Suffix an Zahl mit Punkt oder Exponent (`5.0i32`) und `-0x...` werden abgelehnt (Grammatik erlaubt das erste formal).
- Spec-Auslegung in M0.3: `&` nur als letztes Suffix, `??` verboten (C# erlaubt beides nicht).
- Der deklarierende Typ einer Member-Referenz ist laut Spec nur ein `qname`. Members generischer Typinstanzen (`List<i32>::Add#i32`) lassen sich so nicht ausdrücken, nur über die offene Form (`System.Collections.Generic.List::Add#T`?). Spätestens vor M2 klären (Spec-Änderung braucht ADR).
- Spec-Auslegung in M0.4, bitte bestätigen (Details in `docs/modules/ir.md`):
  - Unbekannte Keywords nehmen das folgende Element als Wert mit, wenn es kein Keyword ist. Spec Abschnitt 11 sagt nur „ignorieren". Folge: Künftige Keywords sollten immer einen Wert haben, sonst schlucken sie z. B. einen Methodenrumpf. Eventuell als Satz in Spec Abschnitt 11 aufnehmen (ADR).
  - `(copy expr)` wird gelesen, obwohl es nur in den Regeln von Abschnitt 7 und nicht in der Grammatik steht.
  - Strengere Prüfungen: Modifier nur aus der Spec-Liste (C# kennt auch `extern`, `new`, `volatile`, `async` usw., das wird ab M2 relevant), `new` nur mit `.ctor`, `default`/`finally` als letzter Zweig, `:version` muss 1 sein.
  - `:attrs` gibt es laut Grammatik nur an Typen und Feldern. An Methoden wird es als unbekanntes Keyword ignoriert (Datenverlust ab M2 möglich).
- AST-Records enthalten `SourceSpan`, `equals` vergleicht also Positionen mit. Struktureller Vergleich ohne Positionen gibt es bisher nur als Test-Helfer (`AstAssertions`). Falls Index/Overlays (ab M4/M6) ihn im Produktionscode brauchen, einen expliziten Normalisierer bauen.
- Spec-Auslegung in M0.5, bitte bestätigen: Events stehen in der Mitgliedsreihenfolge nach Properties (Spec nennt sie nicht), Enum-Werte nach Namen sortiert (Spec: „Mitglieder nach ID"), leere optionale Listen entfallen, Kommentare gehen beim Drucken verloren, `:loc`/`:manual`/`:note` stehen am Knotenende (auch hinter Rumpf und Mitgliedern).
- Spec-Auslegung in M0.6: Externe Typen ohne Katalog als `info` IR002, einmal pro Typname. Lokale Variablen und Sprungmarken werden nicht auf Auflösung geprüft (keine Regel dafür in IR001–IR005).

## Blockiert durch offene Entscheidung

- Siehe `docs/decisions.md`, Einträge mit Status `OPEN`. Aufgaben, die davon abhängen, sind in der Aufgabendatei markiert.

## Hinweise für die nächste Session

- Vor dem Start die Checkliste in `docs/setup.md` prüfen.
- Layout und Projektarten: `docs/projects.md` lesen (Spiele in `games/`, Engines in `engines/`, ADR-020 PROPOSED).
- M0 ist fertig. Vor M1: offene Punkte und `PROPOSED`-ADRs für M1 (006, 007, 016) mit dem Nutzer klären, Branch `task/M0-skeleton` nach `main` mergen (Nutzer entscheidet).
