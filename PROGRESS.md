# PROGRESS.md

Wird von Agents am Ende jeder Session aktualisiert. Kurz und konkret halten.

## Aktueller Stand

- Phase: M0 (IR), M0.1 bis M0.3 erledigt
- Aktive Aufgabe: `tasks/M0-ir.md`, nächste Teilaufgabe M0.4 (AST und Reader für Deklarationen, Statements, Ausdrücke), Branch `task/M0-skeleton`
- Letzte Session: 2026-10-13, M0.2 (Lexer und S-Expression-Reader) und M0.3 (Typ- und Member-Atome)

## Erledigt

- Setup: Git-Repo (`main`), Agent-Kit committet, `dev.example` → `io.github.localsoul`, `.gitignore` (inkl. `original/`). Ordner `original/` (Papers, Please) auf Wunsch des Nutzers gelöscht.
- M0.1 Repo-Skelett: Parent-POM `io.github.localsoul:recomp-parent` (Java 25, UTF-8, alle Plugins gepinnt, `project.build.outputTimestamp` fest), Modul `ir`, Maven-Wrapper 3.3.4 (only-script, Maven 3.9.16), Spotless (google-java-format 1.36.1, `check` in `verify`), Enforcer (Java ≥ 25, Maven ≥ 3.9, gepinnte Plugins, Dependency-Konvergenz, Verbot von Lombok/JUnit 4/Logging-Bibliotheken; in `ir` zusätzlich keine Projektmodule), JUnit 5.14.4, ArchUnit 1.5.0 (Test-Scope), Platzhaltertest.
- M0.2 Lexer und S-Expression-Reader (Paket `io.github.localsoul.port.ir.sexpr`): `IrLexer`, `SExprReader`, `SExpr`-Knoten mit `SourceSpan`, `IrException`/`IrSyntaxException`. 73 Tests. Folgt ADR-004 (handgeschriebener Reader, Status weiter `PROPOSED`). Platzhaltertest entfernt.
- M0.3 Typ- und Member-Atome (Paket `io.github.localsoul.port.ir.atom`): `TypeRef` (sealed: Primitive, benannt/generisch, Array, Nullable, ByRef), `MemberRef` (stabile Symbol-ID), Parser mit Fehlerposition im Atom. Insgesamt 134 Tests.

## Offene Punkte / Nebenfunde

- .NET SDK defekt/unvollständig: `dotnet --version` meldet `/usr/lib64/dotnet/host/fxr does not contain any version-numbered child folders`. Vor M2 beheben.
- Zielspiel wird ein selbst kompiliertes 2048 (Unity, möglichst Mono-Build, siehe ADR-003). Noch nicht vorhanden.
- Statische Prüfung (Error Prone oder SpotBugs, `docs/conventions.md`) ist noch nicht im Build. Nicht Teil von M0.1; Wahl offen, vor M0-Abschluss klären.
- Die SLF4J-Warnung „No SLF4J providers were found“ beim Build stammt von Werkzeugen im Build und ist harmlos.

- Spec-Auslegung in M0.2, bitte bestätigen (Details in `docs/modules/ir.md`):
  - Ganzzahlen ohne Suffix: entschieden in ADR-024 (`nat` nur in `:version`, `(char ...)`, `:size`), Spec angepasst.
  - Die Spec definiert `exp` nicht. Umgesetzt als `e`/`E`, optional `+`/`-`, Ziffern.
  - Ganzzahl-Suffix an Zahl mit Punkt oder Exponent (`5.0i32`) und `-0x...` werden abgelehnt (Grammatik erlaubt das erste formal).
- Spec-Auslegung in M0.3: `&` nur als letztes Suffix, `??` verboten (C# erlaubt beides nicht).
- Der deklarierende Typ einer Member-Referenz ist laut Spec nur ein `qname`. Members generischer Typinstanzen (`List<i32>::Add#i32`) lassen sich so nicht ausdrücken, nur über die offene Form (`System.Collections.Generic.List::Add#T`?). Spätestens vor M2 klären (Spec-Änderung braucht ADR).
- Wertebereich von Zahlen (`300u8`) wird in M0.2 nicht geprüft, vorgesehen für M0.4.

## Blockiert durch offene Entscheidung

- Siehe `docs/decisions.md`, Einträge mit Status `OPEN`. Aufgaben, die davon abhängen, sind in der Aufgabendatei markiert.

## Hinweise für die nächste Session

- Vor dem Start die Checkliste in `docs/setup.md` prüfen.
- Layout und Projektarten: `docs/projects.md` lesen (Spiele in `games/`, Engines in `engines/`, ADR-020 PROPOSED).
- Mit `tasks/M0-ir.md` beginnen. Repo-Skelett (Maven-Multi-Module, Wrapper, Spotless, Enforcer) ist Teil von M0.
