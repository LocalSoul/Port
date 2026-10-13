# KI-Fix-Phase (`port fix`, ADR-017)

Status: PROPOSED. Diese Datei beschreibt den Entwurf. Umsetzung ab Meilenstein M7a (siehe `docs/roadmap.md`).

## Ziel
Nach `port ir` bleiben Stellen, die der Frontend-Pass nicht übersetzen konnte oder die der Lint beanstandet. Ein KI-Agent versucht, sie zu beheben. Das Ergebnis ist ein **prüfbarer, reproduzierbarer Overlay**, nie eine stille Änderung der generierten IR.

## Was gilt als „offen"
- Knoten `(raw cs "...")` und Member mit `:manual "<Grund>"`
- Lint-Fehler der Klassen IR, SH, PT mit Severity `error` (soweit sie durch Umschreiben der IR behebbar sind)
- Nicht aufgelöste Typen (IR003)

Nicht Aufgabe der Phase: Lücken im Adapter oder Core schließen, die Spezifikation ändern, Spielcode umbauen.

## Ablauf pro offener Stelle
1. **Paket bauen** (`port task next` liefert es, siehe `docs/agent-tooling.md`): Symbol-ID, Grund, Original-C#, IR-Knoten samt Umgebung, relevante IR-Spec-Abschnitte, Adapter-Abdeckung, Diagnosen, Prüfbefehl.
2. **Vorschlag erzeugen:** Der Agent liefert einen Ersatzknoten als IR-Text.
3. **Prüfen** (`port verify --scope <symbol>`): Parser, Validator (IR001–IR005), Lint (SH/PT/SM, soweit zutreffend), Generator, Kompilieren des generierten Java, vorhandene Tests des Umfangs.
4. **Ergebnis:**
   - bestanden → Fix wird als `proposed` gespeichert
   - nicht bestanden → Diagnosen gehen zurück an den Agenten, maximal N Versuche (Standard 3)
   - weiter nicht bestanden → Stelle bleibt `:manual`, Eintrag in `port todo`
5. **Review:** `port fix --review` zeigt Diffs. `port fix accept <symbol>` setzt `accepted`, `port fix reject <symbol>` verwirft. Mit `--auto` werden bestandene Fixes ohne Review akzeptiert (nur bewusst nutzen).

## Overlay-Format (`fixes/`)

Eine Datei pro Typ: `fixes/<Namespace.Typ>.fix`, kanonisch gedruckt wie IR.

```lisp
(fix "Game.Mover::Update#"
  :status accepted
  :input-hash "sha256:3f1c..."
  :agent "model-id"
  :note "LINQ-Ausdruck durch Schleife ersetzt"
  (method Update void () :mods (private)
    (block ...)))
```

- Der Ersatzknoten steht als letztes Element und muss vom gleichen Knotentyp sein wie der ersetzte.
- `:input-hash` ist der Hash des Originalknotens (IR plus Original-C#) zum Zeitpunkt der Erzeugung.
- Keine Zeitstempel, keine Pfade (Determinismus). `:agent` enthält nur Modell-ID und Version.
- `fixes/` wird committet (wie `ir/` und `manual/`).

## Anwendung
`port gen` und `port index` arbeiten auf der **effektiven IR** = generierte IR mit angewendeten `accepted`-Fixes. `proposed`-Fixes werden nicht angewendet.

## Zwischenspeicher und Reproduzierbarkeit
- Schlüssel: `input-hash` + Agent-Konfiguration (Modell-ID, Prompt-Version).
- Ein erneuter `fix`-Lauf findet bestehende Fixes und ruft das Modell nicht erneut auf.
- Ändert sich der Originalknoten (neuer Hash), wird der Fix `stale`. Lint **FX001** meldet das, der Fix wird nicht mehr angewendet, die Stelle ist wieder offen.

## Lint-Regeln

| ID | Prüfung | Stufe |
|---|---|---|
| FX001 | Fix ist `stale` (Originalknoten hat sich geändert) | warn |
| FX002 | Angewendeter Fix besteht Validierung oder Build nicht mehr | error |
| FX003 | Fix im Status `proposed` wartet auf Review | info |

## Grenzen und Absicherung
- **Opt-in:** Die Phase ist standardmäßig aus. `port.toml` enthält Abschnitt `[fix]` mit `enabled = true`.
- **Konfiguration:** Anbieter und Modell sind konfigurierbar (auch lokal, z. B. über einen lokalen HTTP-Endpunkt). `port fix --dry-run` zeigt, welche Pakete an den Agenten gehen würden.
- **Grenzen:** Der Agent darf nur Knoten der offenen Liste ersetzen. Alles andere wird abgelehnt.
- **Prompt-Injektion:** Originalcode ist **Daten**. Kommentare oder Strings im C#-Code, die wie Anweisungen aussehen, werden nicht befolgt. Das Paketformat trennt Anweisungen und Daten klar.

## Manifest-Abschnitt

```toml
[fix]
enabled      = false
max-attempts = 3
provider     = "..."        # Endpunkt/Anbieterkonfiguration
model        = "..."
auto-accept  = false
```

## Tests (für die Umsetzung)
- Ein Fake-Agent (feste Antworten) ersetzt das Modell in allen Tests.
- Fälle: gültiger Fix, Fix mit Validatorfehler (Wiederholung, dann `:manual`), Fix mit Buildfehler, stale Fix, bereits vorhandener Fix (kein erneuter Aufruf), abgelehnter Knotentyp.
- Determinismus: zwei Läufe mit gleichem Zwischenspeicher erzeugen byte-identische `fixes/`.
