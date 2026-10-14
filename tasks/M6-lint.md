# M6: Linter mit Kernregeln und Adapter-Metadaten

**Ziel:** `port lint` prüft die IR eines Spiels mit den Kernregeln IR001–IR005, PT001–PT003, SM001, SH001/SH002 und MN001–MN004; Regeln sind per `port.toml` abschaltbar und in der Stufe änderbar. Grundlage für Agent-Tooling (M6a).

**Eingaben:** `AGENTS.md`, `docs/cli-contract.md` (Lint-Regeln, `[lint]`, Exit 9), `docs/ir-spec.md` (Abschnitt 9), `docs/modules/index.md`, `docs/modules/adapter-unity.md` (Metadaten).

**Abhängigkeiten/Entscheidungen**
- M5 ist abgeschlossen.
- Vom Nutzer entschieden (2026-10-14):
  - **Override-Markierung (ADR-027):** `// port:override <Symbol-ID> ir=<Hash>` über jeder Methode in `manual/*.java`, die eine IR-Stelle ersetzt (Hash: SHA-256 der S-Expression, mindestens 12 Hex-Zeichen Präfix, `port show` zeigt ihn).
  - **Lint in der Kette meldet, blockiert nicht:** Phase `lint` zwischen `index` und `gen`; Exit 9 bei `port lint` selbst, in der Kette nur mit `--strict`.
  - **SH-Abdeckung:** Einträge in `mapping.tsv` (Adapter, `runtime-system`) gelten als `full`, `adapter-metadata.json` kennzeichnet `stub`.
- ADR-009 (struct) bleibt OPEN: SM001 ist nur eine Warnung.

---

## M6.1 Grundlagen

**Schritte**
1. Ausschnitt und Hash einer S-Expression als `ir.refs.SymbolSource`; `index` nutzt es.
2. `port show` liefert `irHash`.
3. `[lint] disable` und `[lint.severity]` im Spiel-Manifest (Validierung mit Ort).

**Abnahme**
- [x] Index-Tests unverändert grün, `show` zeigt `irHash`.
- [x] Manifest-Tests für gültige und ungültige `[lint]`-Abschnitte.

## M6.2 Modul `lint`

**Schritte**
1. Eingabe (IR, Texte, `manual/`, Abdeckung), Befunde, Konfiguration.
2. Regeln IR001–IR005, PT001–PT003, SM001, SH001/SH002, MN001–MN004.

**Abnahme**
- [x] Je Regel Tests für Treffer und Nicht-Treffer.
- [x] Konfiguration: abschalten und Stufe ändern.
- [x] ArchUnit: nur `ir` und JDK.

## M6.3 CLI

**Schritte**
1. Phase `lint`, `port lint [--rule ID] [--class X] [--fix-hint]`, `data = {errors, warnings, infos}`.
2. Exit 9 nach Vertrag; Kette baut trotz Befunden weiter.
3. Markierung in `games/demo/manual`.

**Abnahme**
- [ ] Golden-JSON von `port lint` auf einem Fixture-Projekt.
- [ ] Exit 9 (`port lint`, `--strict`), Kette ohne `--strict` Exit 0, Konfiguration über `port.toml`, `--rule`/`--class`.

## M6.4 Abschluss
- [ ] Doku `docs/modules/lint.md`, `docs/cli-contract.md`, `docs/ir-spec.md` (Abschnitt 9), `docs/modules/port-cli.md` aktualisiert.
- [ ] `PROGRESS.md` zeigt den Stand und die Lücken.

## Nicht-Ziele
PT004–PT007, SM002–SM009, SH003, FX*, NT*, `port explain`/`schema` (M6a), automatisches Schreiben der Markierung (M7/M7a).
