# Entscheidungen (ADR-Log)

Status: `ACCEPTED` (gilt), `PROPOSED` (Vorschlag, bis zur Freigabe vorläufig), `OPEN` (noch nicht entschieden), `SUPERSEDED`.

**Regel für Agents:** Bei `PROPOSED` darfst du dem Vorschlag folgen, solange du ihn hinter einem Interface kapselst und im Bericht darauf hinweist. Bei `OPEN` entscheidest du nicht, sondern fragst den Nutzer oder lieferst Optionen. Neue Entscheidungen werden hier ergänzt, nicht im Code versteckt.

Format pro Eintrag: Kontext, Optionen, Empfehlung/Entscheidung, Folgen.

## Übersicht

| ADR | Thema | Status | Spätestens klären vor |
|---|---|---|---|
| 001 | Java 25, Maven Multi-Module | ACCEPTED | – |
| 002 | IR als S-Expressions | ACCEPTED | – |
| 003 | Nur Unity-Mono im Scope | ACCEPTED, ab M10b abgelöst durch 023 | – |
| 004 | Handgeschriebener IR-Reader statt ANTLR | PROPOSED | M0 |
| 005 | Unity-Frontend als separater .NET-Prozess | PROPOSED | M2 |
| 006 | Datenmodell des Cores (ECS, siehe 016) | PROPOSED | M1 |
| 007 | Koordinatenkonvention | PROPOSED | M1 |
| 008 | Coroutines | OPEN | M3 (Coroutines selbst ab M10) |
| 009 | struct-Semantik im Java-Backend | OPEN | M3 |
| 010 | Neutrales Asset-/Szenenformat | OPEN | M8 |
| 011 | Stabilität der Engine-API | OPEN | M5 |
| 012 | IR und Mehrsprachigkeit (GDScript) | OPEN | M11 |
| 013 | Erstes Zielspiel 2D oder 3D | OPEN | M9 |
| 014 | Rendering-Backend des Cores | OPEN | M9 |
| 015 | Subsystem-Provider und Mischbarkeit | PROPOSED | M1 (Grundlagen), M12 |
| 016 | ECS-Entwurf | PROPOSED | M1 |
| 017 | KI-Fix-Phase | PROPOSED | M7a |
| 018 | Agent-first Tool-Design | PROPOSED | M6a |
| 019 | BCL-Nachbau und Typabbildung | PROPOSED (Teilfrage Unsigned-Typen OPEN) | M3a |
| 020 | Monorepo, Projektarten Game-Recomp und Engine-Recomp, Katalog | PROPOSED | M0 (Layout), M4a |
| 021 | Technik der Launcher-Oberfläche | OPEN | M12a |
| 022 | Engine-Recomp aus Binärdateien, funktionale Gleichheit | PROPOSED | M11a |
| 023 | Nativer Code: Disassemblieren und Heben, wann immer möglich | ACCEPTED (Richtung), Umsetzung PROPOSED | M10b |
| 024 | Ganzzahlen ohne Typsuffix (`nat`) in der IR | ACCEPTED | M0 |

---

## ADR-001: Java 25 und Maven Multi-Module
**Status:** ACCEPTED
Toolchain, Engine-API, Adapter und Generator sind Java 25. Build mit Maven (Wrapper), ein Multi-Module-Projekt.
**Folgen:** Eine Sprache, eine IDE-Erfahrung, klassisches Debugging.

## ADR-002: IR als S-Expressions
**Status:** ACCEPTED
Textformat laut `docs/ir-spec.md`: aufgelöst, entzuckert, kanonisch, diffbar.

## ADR-003: Nur Unity-Mono im Scope
**Status:** ACCEPTED
IL2CPP-Spiele sind vorerst nicht im Scope (kaum sauber dekompilierbar).
**Änderung:** Durch ADR-023 gilt nativer Code als Eingabe für alle Recomps. IL2CPP-Spiele kommen damit grundsätzlich in Reichweite, sobald das native Frontend (M10b) steht. Bis dahin gilt ADR-003 unverändert.

## ADR-004: Handgeschriebener S-Expression-Reader statt ANTLR
**Status:** PROPOSED
**Kontext:** Die Grammatik ist S-Expression-basiert (Lexik und Liste), nur Typ- und Member-Atome haben eine kleine Untersyntax.
**Optionen:** (a) ANTLR-Grammatik, (b) handgeschriebener Reader für Listen/Atome plus kleiner Parser für Typ- und Member-Atome.
**Empfehlung:** (b). Bessere Fehlermeldungen mit Position, keine Generator-Abhängigkeit, leichter debugbar.
**Folgen:** Parser und Printer sind handgepflegt, Roundtrip-Tests sichern sie ab.

## ADR-005: Unity-Frontend als separater .NET-Prozess
**Status:** PROPOSED
**Kontext:** ILSpy/ICSharpCode.Decompiler und Roslyn sind .NET-Bibliotheken.
**Optionen:** (a) separater .NET-Konsolenprozess, der IR-Text auf stdout oder in eine Datei schreibt, (b) in-process Brücke (JNI/GraalVM), (c) Java-eigener IL-Reader.
**Empfehlung:** (a). Einfache Schnittstelle (Datei/stdout), Java-Wrapper startet den Prozess.
**Folgen:** Das .NET SDK ist Voraussetzung für Frontend-Tests, die Tests müssen ohne SDK sauber übersprungen und im Bericht benannt werden.

## ADR-006: Datenmodell des Engine-Cores
**Status:** PROPOSED (Entwurf: ECS mit Sparse-Set-Speicher, Details in ADR-016)
**Optionen:** (a) ECS mit Entities als IDs und Komponenten in Speichern, (b) einfacher Entity-Baum mit Komponenten pro Entity.
**Kriterium:** Beide fremden Modelle (Unity GameObject/Component, Godot Node-Baum) sollen sich sauber abbilden lassen, ohne dass der Core eines übernimmt.
**Vorgehen für Agents:** Dem Entwurf aus ADR-016 folgen, aber immer hinter den Interfaces in `engine-api` (`World`, `Entity`, `ComponentStore`) implementieren, damit der Speicher später austauschbar bleibt. Widersprechen die Erkenntnisse beim Bauen dem Entwurf, anhalten und den Nutzer fragen.

## ADR-007: Koordinatenkonvention des Cores
**Status:** PROPOSED
**Vorschlag:** rechts-händig, Y oben, Einheit Meter, Winkel in Radiant intern. Der Unity-Adapter spiegelt an der Grenze (Z-Achse). Der Godot-Adapter braucht keine Konvertierung.
**Folgen:** Alle Konvertierungen leben ausschließlich in Adaptern, nie im Core.

## ADR-008: Coroutines
**Status:** OPEN
**Optionen:** (a) `yield-return` bleibt in der IR, der Generator baut eine State Machine, (b) `yield-return` bleibt in der IR, der Adapter führt Coroutines mit Virtual Threads aus.
**Stand:** Die IR behält `yield-return` in beiden Fällen (siehe IR-Spec).
**Vorgehen für Agents:** In M3 erst einfache Methoden ohne Coroutines, Coroutines nicht umsetzen, bis entschieden ist.

## ADR-009: struct-Semantik im Java-Backend
**Status:** OPEN
**Optionen:** (a) implizit wie in C#, der Generator fügt Kopien ein, (b) ein Pass fügt explizite `(copy ...)`-Knoten in die IR ein, der Generator setzt sie 1:1 um.
**Hinweis:** Lint SM001 existiert unabhängig davon.

## ADR-010: Neutrales Asset-/Szenenformat
**Status:** OPEN
**Optionen:** JSON, RON oder ein eigenes kompaktes Format.
**Hinweis:** Jeder Adapter hat einen eigenen Loader. Das Format ist engine-neutral und liegt unter `data/`.

## ADR-011: Stabilität der Engine-API
**Status:** OPEN
**Frage:** Ab wann wird die Engine-API versioniert und eingefroren (Semantic Versioning)? Bis dahin gilt: Änderungen sind erlaubt, aber nur mit Eintrag hier und Anpassung aller Adapter im selben Commit.

## ADR-012: IR und Mehrsprachigkeit
**Status:** OPEN
**Frage:** Reicht die C#-nahe IR für GDScript (dynamisch typisiert)? Möglicherweise nötig: `:lang`-Attribut, Knoten für dynamische Zugriffe.
**Vorgehen:** Erst bei Beginn des Godot-Frontends entscheiden.

## ADR-013: Erstes Zielspiel 2D oder 3D
**Status:** OPEN
**Hinweis:** Bis zur Entscheidung nur 2D-neutrale Grundlagen im Core (Transform, Szene, Input). Physik und 3D-Rendering warten.

## ADR-014: Rendering-Backend des Cores
**Status:** OPEN
**Hinweis:** Bis zur Entscheidung nur eine Null-Rendering-Implementierung (headless) hinter einem Interface. Kein Fenster-/Grafikcode.

## ADR-015: Subsystem-Provider und Mischbarkeit
**Status:** PROPOSED
**Kontext:** Langfristiges Ziel ist, Engines zu mischen (z. B. Rendering-Verhalten wie Unity, Eingabe-Handling wie Godot) und pro Subsystem auszuwählen.
**Entscheidungsvorschlag:**
1. **Zwei Achsen:** Der **Adapter** bestimmt die API-Semantik, die das Spiel erwartet (z. B. Unity-`Input.GetAxis` mit Glättung). **Provider** pro Subsystem bestimmen das Verhalten dahinter (z. B. `render-unity-like`, `input-godot-like`).
2. **Subsysteme sind getrennte Interfaces** in `engine-api` (`Input`, `Renderer`, `Physics`, `Audio`, ...), keine einzelne Fassade.
3. **Subsysteme reden nie direkt miteinander.** Datenaustausch läuft über den Core:
   - gemeinsamer Zustand in der Welt: Komponenten mit neutralen, versionierten Schemas (`Transform`, `Renderable`, `Collider`, `AudioSource`, ...),
   - Events/Commands über einen Event-Bus mit deterministischer Reihenfolge,
   - Scheduler-Phasen mit definierten Synchronisationspunkten.
4. **Besitzer pro Komponente:** Genau ein Subsystem schreibt eine Komponente, andere lesen. Das wird beim Start geprüft.
5. **Jeder Provider deklariert** per `SubsystemInfo`, welche Komponenten er liest/schreibt und welche Events er sendet/empfängt, plus Pflichtpaare und Konflikte.
6. **Auswahl** per `port.toml` (`[engine.subsystems]`), Auflösung über `ServiceLoader`. `port engine check` prüft die Kombination anhand der Deklarationen.
7. **Profile vor Providern:** Unterschiede wie „Unity-artig" vs. „Godot-artig" werden zuerst als Parameter-Profile eines Providers gelöst, eigene Provider nur bei Bedarf.
**Grenzen:**
- „Unity-Rendering" bedeutet Verhalten nachbilden (Farbraum, Beleuchtungsmodell, Tonemapping-Defaults, Kamera-/Layer-Konventionen), **nicht** Unitys Renderer verwenden (Closed Source).
- Leicht mischbar: Input, Audio, Zeitsteuerung. Schwierig: Rendering, Szene, Physik (gemeinsames Transform-/Szenenmodell). Solche Kopplungen werden in der Provider-Deklaration als Pflichtpaare oder Konflikte ausgedrückt.
- Die Adapter-Semantik (z. B. Unity-Glättung bei `GetAxis`) bleibt im Adapter, Provider liefern Rohdaten und Eingabegefühl.
**Folgen:** Schemas sind Teil der Engine-API und unterliegen ADR-011. Das Datenmodell (ADR-006) bestimmt, wie sauber sich die Verträge ausdrücken lassen.
**Vorgehen für Agents:** In M1 Subsysteme getrennt halten, Event-Bus und `SubsystemInfo` einführen, Standard-Provider im `engine-core` belassen. Weitere Provider-Module und `port engine`-Befehle kommen erst in M12.
**Offene Teilfragen:** Welche Komponentenschemas gehören in Version 1? Semantik des Event-Bus (sofort oder gepuffert pro Phase)? Wie werden Kopplungen deklariert (Pflichtpaare, Konflikte)?

## ADR-016: ECS-Entwurf für den Engine-Core
**Status:** PROPOSED (konkretisiert ADR-006)
**Entwurf:**
1. **Entity** ist eine 64-Bit-ID aus Index und Generation. Veraltete Handles werden erkannt.
2. **Komponenten** sind reine Datenrecords mit versioniertem Schema (siehe ADR-015), je Typ ein **Sparse-Set-Speicher** hinter dem Interface `ComponentStore`. Ein Wechsel auf Archetypen bleibt später möglich, ohne die API zu ändern.
3. **Systeme** sind die Subsystem-Provider (ADR-015). Sie laufen in Scheduler-Phasen und deklarieren Lesen/Schreiben in `SubsystemInfo`.
4. **Hierarchie** über `Parent` und geordnete `Children`. Ein Transform-System berechnet `WorldTransform` aus lokalen Transformationen.
5. **Strukturänderungen** (Entity erzeugen/zerstören, Komponente hinzufügen/entfernen) während einer Iteration landen in einem **Command-Buffer** und werden am Phasenende ausgeführt.
6. **Abfragen** sind explizite Iteration in aufsteigender Entity-Reihenfolge (deterministisch). Keine Lambda-Ketten.
7. **Der Core kennt keine Skripte.** Fremde Objektzustände (z. B. Unity-`MonoBehaviour`-Instanzen) leben im Adapter. Der Adapter registriert einen eigenen Komponententyp (Erweiterungspunkt, z. B. `ScriptHost` mit mehreren Skriptinstanzen und opakem Handle), der Core speichert ihn nur.
8. **Debugbarkeit:** deterministisches `World.dump()`, `EntityView` (alle Komponenten einer Entity lesbar), benannte Komponentenklassen.
**Abbildung fremder Modelle:**
- Unity: `GameObject` = Entity plus `Name`, `Active`, `Layer`, `Tag`. Mehrere Skripte desselben Typs pro Objekt laufen über den adapterseitigen `ScriptHost`.
- Godot: Knotenbaum = Entities mit `Parent`/`Children`, Knotentyp = Komponentenmenge, Signals = Events, Gruppen = Tag-Komponenten.
**Alternative:** Einfacher Entity-Baum. Leichter zu debuggen, aber das Mischen von Subsystemen (ADR-015) ist weniger sauber.
**Offene Teilfragen:** Archetypen statt Sparse-Set bei Performanceproblemen? Wie viele Komponenten pro Typ und Entity (genau eine, außer Erweiterungstypen)?

## ADR-017: KI-Agent-Phase `fix` nach `ir`
**Status:** PROPOSED
**Kontext:** Nach `ir` bleiben offene Stellen: `(raw cs ...)`, `:manual` und Lint-Fehler. Ein KI-Agent soll sie, wo möglich, beheben.
**Entwurf:** Details in `docs/ai-fix-phase.md`.
1. Phase `port fix` direkt nach `ir`. Pro offener Stelle erhält der Agent ein Arbeitspaket (Original-C#, IR-Knoten, Regeln, Adapter-Abdeckung).
2. **Fixes liegen als Overlay** in `fixes/`, adressiert über die stabile Symbol-ID. Die generierte IR wird nie direkt bearbeitet, damit `ir --update` sie nicht überschreibt.
3. Jeder Fix wird **vor Annahme geprüft** (Validator, Lint, Build, vorhandene Tests des Umfangs). Nach N Fehlversuchen bleibt die Stelle `:manual`.
4. **Reproduzierbar:** Akzeptierte Fixes werden gespeichert und über den Hash der Eingabe zwischengespeichert. Ein erneuter Lauf braucht das Modell nicht. Ändert sich der Originalknoten, wird der Fix `stale` (Lint FX001).
5. **Review-Gate:** Standard ist manuelle Annahme. `--auto` nur, wenn alle Prüfungen grün sind.
6. **Opt-in:** Die Phase ist standardmäßig aus. Modell und Anbieter sind konfigurierbar (auch lokal).
7. Der Agent ändert nur Knoten der offenen Liste und nie die Spezifikation.
**Folgen:** Neuer Befehl `port fix`, neue Lint-Regeln FX001–FX003, neuer Exit-Code 10.

## ADR-018: Agent-first Tool-Design des CLI
**Status:** PROPOSED
**Entwurf:** Details in `docs/agent-tooling.md`.
1. Das CLI ist primär für AI-Agents gebaut. Menschenlesbarer Text ist die zweite Darstellung derselben Ergebnisobjekte.
2. **Selbstbeschreibend:** `port schema <befehl>`, `port explain <regel-id>`, Diagnosen mit vorgeschlagenem nächstem Befehl.
3. **Arbeitspakete:** `port task next` liefert genau ein Paket mit allem, was der Agent braucht, begrenzt per `--budget`.
4. **Verifikation:** `port verify --scope <symbol|klasse>` prüft schnell und gibt minimale Ausgabe zurück.
5. **Transaktionale Änderungen:** `port patch apply` validiert, schreibt atomar und rollt zurück. Adressierung über Symbol-IDs, nicht Zeilennummern. Alle schreibenden Befehle kennen `--dry-run`.
6. **MCP-Server:** `port mcp` stellt eine kleine Menge Tools bereit (`task_next`, `show_symbol`, `explain_rule`, `propose_fix`, `verify`, `accept_fix`, `reject_fix`).
7. **Leitplanken:** Standardmäßig lesend, Schreibzugriff nur auf `ir/`, `manual/`, `fixes/`, kein Netzwerkzugriff.
8. **Ausgabegröße:** Paginierung mit `--limit`/`--cursor` und Hinweis bei Kürzung.
**Folgen:** CLI-Vertrag wird erweitert, `port-cli` bekommt ein Paket `agent/`.

## ADR-019: BCL-Nachbau und Typabbildung
**Status:** PROPOSED (Teilfrage Unsigned-Typen OPEN)
**Kontext:** Spielcode nutzt neben der Engine-API auch das .NET-Basisbibliothek-Subset (`System.*`: Collections, Strings, Math, Exceptions). Der Generator braucht dafür eine Java-Entsprechung.
**Entwurf:**
1. Neues Modul `runtime-system` mit Java-Implementierungen des benötigten `System.*`-Ausschnitts. Es hängt von keinem anderen Projektmodul ab.
2. Abbildung über eine `mapping.tsv` (IR-Symbol → Java-Symbol), wie beim Adapter. Namenskonvention: Methoden `lowerCamelCase`, Properties als Methoden, Indexer als `get`/`set`, Operatoren als benannte statische Methoden.
3. Bedarfsgesteuert wachsen: nur, was `port deps check`/Lint als genutzt meldet.
4. Semantikunterschiede werden im Modul dokumentiert und getestet (Beispiele in `docs/type-mapping.md`).
**Offene Teilfrage:** Abbildung der Unsigned-Typen (`u8`, `u16`, `u32`, `u64`): (a) nächstgrößerer signierter Typ, (b) gleicher Bitbreite mit Hilfsmethoden, (c) Wrapper-Typen. Bis zur Entscheidung erzeugt der Generator `todo` und der Lint meldet SM002.
**Folgen:** Generierter Code importiert Adapter und `runtime-system`. ArchUnit-Regeln in `AGENTS.md` berücksichtigen das.

## ADR-020: Monorepo mit zwei Projektarten und Launcher-Profilen
**Status:** PROPOSED
**Kontext:** Es gibt zwei verschiedene Aufgaben: die Spiellogik eines Spiels portieren (Game-Recomp) und eine fremde Engine auf der Engine-API nachbilden (Engine-Recomp). Alle Projekte sollen in einem Repo liegen, und am Ende soll ein Launcher Spiel, Engine und Subsysteme kombinieren und starten.
**Entwurf:** Details in `docs/projects.md`.
1. Monorepo: Plattform-Module im Root, `engines/<name>/` für Engine-Recomps, `games/<name>/` für Game-Recomps, `profiles/` für Zusammenstellungen.
2. Jedes Projekt hat ein `port.toml` mit `[project] kind = "game" | "engine"`.
3. Spiele deklarieren, was sie brauchen (`[game.requires]`), Engines, was sie bereitstellen (`[engine.provides]`). Die Verbindung entsteht nur im Profil.
4. Spiele sind keine Module des Root-Reactors, Engine-Module schon.
5. Der Katalog (`port catalog`) findet Projekte über `port.toml`.
6. Der Launcher (`launcher-core`) ist headless und enthält die ganze Logik, eine Oberfläche ruft nur diese Bibliothek.
**Alternativen:** Ein Repo pro Spiel (verworfen, weil Launcher und gemeinsame Werkzeuge sonst verteilt wären); Git-Submodule für große Spiele (bleibt als spätere Option, ohne Layoutänderung möglich).
**Folgen:** Neues Modul `launcher`, neue Befehle `port catalog` und `port launch`, neues Dokument `docs/projects.md`. Der Abschnitt `[target]` im Spiel-Manifest entfällt.

## ADR-021: Technik der Launcher-Oberfläche
**Status:** OPEN
**Optionen:** (a) Desktop-Oberfläche in Java (JavaFX oder Swing), (b) lokale Web-Oberfläche (eingebetteter Server, Browser), (c) Terminal-Oberfläche, (d) nur CLI.
**Hinweis:** Die Entscheidung blockiert nichts bis M12a, denn die gesamte Logik liegt in `launcher-core` (ADR-020).

## ADR-022: Engine-Recomp aus Binärdateien und funktionale Gleichheit
**Status:** PROPOSED
**Kontext:** Eine Engine-Recomp soll nicht nur von Hand entstehen, sondern auch aus Binärdateien der Engine gelesen werden können, mit dem Ziel funktionaler Gleichheit.
**Entwurf:** Details in `docs/projects.md`, Abschnitt 6.
1. **Quellen:** verwaltete Assemblies laufen durch dieselbe Pipeline wie Spiele (Frontend → IR → Generator) mit Ziel „Engine-Modul". Reine API-Stubs und Beobachtung bleiben als Handarbeit möglich.
2. **Gleichheit wird gemessen:** Konformitätstests (`conformance/`) mit Szenarien, Referenzspuren und dokumentierten Toleranzen. Referenzen sind Golden Files.
3. **Neutralität bleibt:** Übersetzter Engine-Code landet im Adapter oder als Provider, nie im Core. Generiertes und Handgeschriebenes bleiben getrennt.
4. **Befehle:** `port conformance record|run|diff`, `port engine verify` (Abdeckung plus Konformität).
**Nativer Code:** entschieden in ADR-023 (Heben, wann immer möglich).
**Folgen:** Frontend und Pipeline brauchen den Zieltyp „Engine-Modul", neue Lint-Regelgruppe `EN` (Engine-Module), neues Verzeichnis `conformance/` in Engine-Recomps.

## ADR-023: Nativer Code wird disassembliert und gehoben, wann immer möglich
**Status:** ACCEPTED (Richtung, vom Nutzer entschieden). Die technische Umsetzung unten ist PROPOSED.
**Kontext:** Nicht jede Eingabe liegt als verwaltete Assembly vor: Engine-Kerne, Physik-/Audio-Bibliotheken, native Plugins und IL2CPP-Spiele sind nativer Code.
**Entscheidung:** Bei **allen** Recomps (Game und Engine) wird nativer Code, wo immer möglich, disassembliert und in die IR gehoben, statt sein Verhalten nur von Hand nachzubauen. Handarbeit mit Konformitätstests (ADR-022) ist der Rückfall, wenn Heben nicht möglich oder nicht tragfähig ist (z. B. verschleierter Code, nicht rekonstruierbare Strukturen). Pro Funktion wird der Weg festgehalten.
**Umsetzung (Entwurf):**
1. **Neues Modul `frontend-native`:** Disassemblieren und Heben in einem **externen Prozess** (wie ADR-005), z. B. mit einem etablierten Disassembler-Framework mit Zwischensprache. Die Wahl des Werkzeugs ist eine eigene Entscheidung (Kandidaten: Ghidra headless mit P-Code, Capstone plus eigener Heber, radare2) und steht vor M10b in `docs/decisions.md`.
2. **Architekturen:** zuerst x86-64, danach ARM64. Weitere nur auf Bedarf.
3. **Zwei Ebenen der IR:** Gehobener Code soll möglichst die normale, strukturierte IR erreichen (Typen, Kontrollfluss, Aufrufe). Wo das nicht gelingt, nutzt die IR ein niedrigeres Dialekt-Fragment (`native`-Knoten: Register/Speicher, Basisblöcke). **Das erfordert eine Erweiterung von `docs/ir-spec.md`.** Nach `AGENTS.md`, Regel 2, wird sie zuerst als eigener Entscheidungsteil freigegeben, bevor Parser und Printer angefasst werden.
4. **Typrückgewinnung:** Symbole, Debuginformationen und Metadaten (z. B. die IL2CPP-Metadaten) liefern Namen und Typen. Fehlende Information führt zu `:manual` bzw. `raw`, nie zu stillen Annahmen.
5. **Absicherung:** Gehobener Code ist nie „automatisch gleich". Konformitätstests (ADR-022) und, wo möglich, **Differenztests auf Funktionsebene** (Eingaben in Original und Recomp, Ausgaben vergleichen) bestätigen ihn.
6. **KI-Fix-Phase:** Dieselbe wie bei verwaltetem Code (ADR-017), mit zusätzlichen Arbeitspaketen für schlecht gehobene Funktionen.
7. **Lint:** neue Regelgruppe `NT` (z. B. NT001 unaufgelöster indirekter Sprung, NT002 Zeigerarithmetik ohne Typ, NT003 Inline-Assembler/SIMD ohne Entsprechung).
**IR-Erweiterung:** vom Nutzer freigegeben, als Abschnitt 12 in `docs/ir-spec.md` ausgearbeitet (Version 1, ein `cfg`-Dialekt mit Speicher- und Aufrufknoten, Regeln IR006–IR010).
**Folgen:** Neues Modul `frontend-native`, neue Pipeline-Eingabeart `native`, Erweiterung der IR-Spezifikation (erledigt, Implementierung in M10b), Meilenstein M10b, ADR-003 wird für IL2CPP abgelöst. Konformitäts- und Differenztests werden Pflicht für gehobenen Code.

## ADR-024: Ganzzahlen ohne Typsuffix (`nat`) in der IR
**Status:** ACCEPTED (vom Nutzer freigegeben, 2026-10-13)
**Kontext:** `docs/ir-spec.md` verlangt für jede Zahl ein Typsuffix (`number`). Die Spec selbst schreibt aber an drei Stellen Zahlen ohne Suffix: `(ir :version 1 ...)`, `(char 97)` und `:size 24` in `:native`. Die Grammatik (`header`, `literal`, `nativeinfo`) nennt dort `number` und widerspricht damit den Beispielen.
**Optionen:** (a) Beispiele ändern (`:version 1i32`, `(char 97u16)`), (b) eigene Lexik `nat` (nicht-negative Ganzzahl ohne Suffix), die nur an genau diesen drei Stellen erlaubt ist.
**Entscheidung:** (b). Die drei Werte sind Metadaten bzw. Code-Units mit festem Typ, ein Suffix trägt keine Information.
**Umsetzung:** Spec Abschnitt 2 bekommt `nat := ziffern`. `header` nutzt `":version" nat`, `literal` nutzt `"(" "char" nat ")"`, `nativeinfo` nutzt `[":size" nat]`. Der Lexer liefert `nat` als eigenes Token (`SBareInteger`), die AST-Ebene lehnt es überall sonst mit „Zahl ohne Typsuffix" ab. Zahlen mit Vorzeichen, Punkt oder Exponent brauchen immer ein Suffix.
**Folgen:** Keine Versionsänderung (Spec Abschnitt 11: noch keine Version veröffentlicht, die Beispiele waren schon so geschrieben). Printer gibt `nat` ohne Suffix aus.
