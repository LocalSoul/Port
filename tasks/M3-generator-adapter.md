# M3: Java-Generator und erster `adapter-unity`

**Ziel:** Das `Mover`-Beispiel läuft End-to-End headless: IR → Java → kompiliert → bewegt eine Transform im `engine-core` wie erwartet.

**Eingaben:** `AGENTS.md`, `docs/ir-spec.md`, `docs/modules/generator-java.md`, `docs/modules/adapter-unity.md`, `docs/modules/engine-api.md`, `docs/decisions.md` (ADR-007, 008, 009).

**Abhängigkeiten/Entscheidungen**
- M0, M1 und M2 sind abgeschlossen.
- **ADR-008 (Coroutines) und ADR-009 (struct-Semantik) sind `OPEN`.** M3 umfasst nur einfache Methoden **ohne Coroutines** und behandelt `struct`-Typen des Adapters (`Vector3`) als unveränderliche Records. Nicht verallgemeinern. Alles Weitere wird als `todo` generiert.
- **ADR-007 (Koordinaten)** `PROPOSED`: Der Unity-Adapter spiegelt an der Grenze.
- M3 nutzt nur Primitive und `string` aus dem BCL. Collections und weitere `System.*`-Typen kommen in M3a (ADR-019). Unsigned-Typen bleiben `todo`.

---

## M3.1 `adapter-unity`, Minimalumfang

**Schritte**
1. Modul `adapter-unity` unter `engines/unity/adapter-unity` (Engine-Recomp, `docs/projects.md`; Maven-Modulname bleibt `adapter-unity`) mit Abhängigkeit nur auf `engine-api` (`engine-core` nur im `test`-Scope).
2. Typen: `MonoBehaviour`, `Component`, `GameObject`, `Transform`, `Vector3` (unveränderlicher Record mit `mul`, `add`, `sub`, `equals`), `Time` (`deltaTime`), `Input` (`getAxis`). `getAxis` ist in M3 ein `stub` und liefert den Rohwert der Achse ohne Unity-Glättung (Metadaten-Status `stub`, Lint SH002). Die echte Glättung (Sensitivity, Gravity) folgt später im Adapter.
3. Lifecycle-Treiber: `Awake`, `Start`, `Update`, `FixedUpdate`, `LateUpdate` auf die Scheduler-Phasen der Engine-API abgebildet. Reihenfolge laut Unity-Dokumentation, **Abweichungen als Kommentar festhalten**.
4. Koordinatenkonvertierung ausschließlich im Adapter (ADR-007).
5. `mapping.tsv` und `adapter-metadata.json` (Format in `docs/modules/adapter-unity.md`).

**Abnahme**
- [x] Lifecycle-Reihenfolge gegen eine dokumentierte Referenzabfolge getestet.
- [x] Ein Test belegt: `Translate(new Vector3(1,0,0))` im Unity-Koordinatensystem ergibt die erwartete Position im Core-Koordinatensystem.
- [x] Konsistenztest: jedes `full`/`stub`-Symbol in den Metadaten hat einen Mapping-Eintrag und umgekehrt.
- [x] ArchUnit: `adapter-unity` importiert nichts aus `engine-core` im `main`-Scope.

## M3.2 Generator, Grundumfang

**Schritte**
1. Modul `generator-java` (Abhängigkeit nur auf `ir`).
2. Java-Emitter (eigener, einfacher Emitter oder JavaPoet, siehe `docs/modules/generator-java.md`).
3. Konstrukte: Klassen, Felder (mit `:init`), Properties, Methoden, `local`, `set`, `if`, `while`, `for`, `return`, `call`, `scall`, `new`, Operatoren (Primitive), Literale.
4. Mapping aus `mapping.tsv` lesen, Operator-Aufrufe auf Adapter-Methoden abbilden.
5. Nicht unterstützte Konstrukte (Coroutines, Events, `raw`) erzeugen `todo`-Blöcke und werden in der Ergebnisliste gemeldet.

**Abnahme**
- [x] Golden-File-Test: IR des `Mover` ergibt das erwartete `MoverBase.java` (`testdata/golden/java/`).
- [x] Generierter Code ist deterministisch und google-java-format-konform.
- [x] Unbekannte Konstrukte erzeugen `todo` mit Symbol und werden gemeldet.
- [x] Der Generator hat keine Abhängigkeit auf `adapter-*` oder `engine-*` (ArchUnit).

## M3.3 End-to-End

**Schritte**
1. Integrationstest-Modul (z. B. `integration-tests`), das generiertes Java kompiliert (`javax.tools.JavaCompiler`) und gegen `adapter-unity` + `engine-core` ausführt.
2. Szene: eine `GameObject` mit `Mover`, skriptierte Eingabe `Horizontal = 1`, 60 Frames bei festem Zeitschritt.

**Abnahme**
- [ ] Nach 60 Frames hat sich die Transform um `speed * Summe(deltaTime)` bewegt, erwarteter Wert exakt berechnet im Test (nicht kopiert aus der Ausgabe). `getAxis` liefert in M3 den skriptierten Rohwert 1.0, die Bewegung ist also von Anfang an voll.
- [ ] Lauf ist deterministisch (zweimal identisches Ergebnis).
- [ ] Der Test nutzt die IR aus M2 (oder deren Golden File) als Eingabe.

## M3.4 Abschluss
- [ ] Doku (`docs/modules/*`) aktualisiert.
- [ ] Fehlende Konstrukte, Mapping-Lücken und Beobachtungen zu Engine-API-Wünschen in `PROGRESS.md`.

## Nicht-Ziele
Coroutines, Events, Delegates, Lambdas mit Capture, Unsigned-Typen (nur mit ADR), Szenen/Prefabs, Physik.
