# SC: StarCollector End-to-End bis zum spielbaren Klon

**Ziel:** Das eigene Spiel des Nutzers (StarCollector, Unity-Mono, `games/starcollector`) läuft über die ganze Pipeline als spielbarer Klon: Fenster, Sprites, HUD-Text, Tastatur, Spielende und Neustart wie im Original. Inhaltlich Roadmap M9, vorgezogen (Nutzerwunsch 2026-10-14).

**Eingaben:** `AGENTS.md`, `games/starcollector/ir/StarCollectorGame.ir`, `docs/decisions.md` (ADR-009, ADR-010, ADR-014, ADR-015), `docs/modules/adapter-unity.md`, `docs/modules/generator-java.md`, `docs/modules/engine-api.md`, `docs/modules/engine-core.md`.

**Entscheidungen (Nutzer, 2026-10-14):** ADR-009 für Engine-Structs ACCEPTED (unveränderliche Records, Mutationen werden Neuzuweisungen); ADR-014 ACCEPTED (modulare `RenderBackend`-Schnittstelle, erstes Backend Swing mit Fenster und Offscreen); ADR-010 bleibt OPEN (Start über `[run] spawn`, Kamera nach Unity-2D-Standard). M6 vorher nach `main` gemergt.

---

## S1 Generator
- [x] Mapping-Formen `setter`, `static-setter`, `constant`, `factory`, `mutator`, Typform `struct`.
- [x] Struct-Mutationen werden Neuzuweisungen; Struct-Arrays und `default(T)` mit `T.DEFAULT`.
- [x] Golden-/Unit-Tests je Regel, google-java-format grün.

## S2 Adapter (Repo `engines/unity`)
- [ ] Alle von StarCollector genutzten Unity-APIs in Adapter, Mapping und Metadaten.
- [ ] `port -C games/starcollector lint` ohne SH001.
- [ ] Tests je Bereich (Structs, Input, Object/Destroy, Texturen/Sprites, GUI).

## S3 Engine (neutral)
- [x] Komponenten für Textur, Sprite, Kamera, UI; Schnittstelle `RenderBackend`; Tasten-/Fenster-Ereignisse.
- [x] `SpriteRenderSystem`, `KeyboardInput`, Frame mit variabler Dauer, Beenden-Anforderung; Tests mit aufzeichnendem Backend.

## S4 Backend Swing
- [x] Modul `render-swing` (nur engine-api, JDK): Fenster und Offscreen; Offscreen-Tests.

## S5 Start
- [ ] `launcher-boot`: `--render`, `--realtime`, `--seed`, `--screenshot`; Profil `starcollector-unity` im Fenster.

## S6 End-to-End
- [ ] Headless-Test über die echte Pipeline (Offscreen, fester Seed, Eingabeskript): Sterne, Einsammeln, Zeitablauf, Neustart, Screenshot-Prüfung.
- [ ] Von Hand spielbar über `port launch run starcollector-unity`.

## S7 Abschluss
- [ ] Doku (ADRs, Module), `PROGRESS.md`; Abnahme durch den Nutzer.

## Nicht-Ziele
Szenen-Import (ADR-010, M8), Audio, Physik, eigene Structs von Spielen, weitere Render-Backends.
