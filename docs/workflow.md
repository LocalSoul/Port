# Arbeitsablauf für Agents

## Pro Aufgabe

1. **Lesen:** `AGENTS.md`, `PROGRESS.md`, die Aufgabendatei in `tasks/`, dann nur die Dokumente, die die Aufgabe nennt.
2. **Prüfen:** Berührt die Aufgabe eine Entscheidung mit Status `OPEN` oder `PROPOSED` in `docs/decisions.md`? Dann zuerst den Nutzer fragen oder einen Vorschlag formulieren, bevor du Code schreibst, der davon abhängt.
3. **Planen:** Bei Aufgaben über mehrere Module einen kurzen Plan aufschreiben (Dateien, Reihenfolge, Tests) und mit dem Nutzer abstimmen.
4. **Branch:** `task/<id>-kurzname`, z. B. `task/M0-ir-parser`.
5. **Test zuerst:** Für jedes Abnahmekriterium einen Test schreiben, der zunächst fehlschlägt. Danach implementieren.
6. **Klein committen:** Nach jedem grünen Schritt ein Commit (siehe `docs/conventions.md`).
7. **Prüfen:** `./mvnw -q verify` (und `dotnet test`, falls .NET berührt wird) muss grün sein.
8. **Dokumentieren:** Betroffene Dokumente aktualisieren. Entscheidungen in `docs/decisions.md` nachtragen.
9. **Abschließen:** `PROGRESS.md` aktualisieren (Stand, Erledigtes, Nebenfunde, Hinweise für die nächste Session).

## Pull-Request-Beschreibung

```
## Aufgabe
tasks/<datei> (Abschnitt/ID)

## Was wurde gemacht
- ...

## Abnahmekriterien
- [x] ...
- [ ] ... (nicht erfüllt, Grund: ...)

## Tests
- ./mvnw -q verify: grün
- neue Tests: ...

## Nicht geprüft / offen
- ...

## Berührte Entscheidungen
- ADR-xxx: ...
```

## Umgang mit Unklarheiten

- **Spezifikation widersprüchlich oder lückenhaft:** nicht raten. Lücke in `PROGRESS.md` unter „Offene Punkte" dokumentieren und den Nutzer fragen.
- **Aufgabe größer als beschrieben:** anhalten, Teilaufgaben vorschlagen.
- **Anderer Code ist kaputt:** nur beheben, wenn er die aktuelle Aufgabe blockiert, sonst als Nebenfund notieren.
- **Neue Abhängigkeit nötig:** begründen und im Plan nennen. Neue Hauptabhängigkeiten brauchen einen ADR.

## Ehrlichkeit im Bericht

Berichte, was du gebaut **und** getestet hast, und was nicht. Wenn ein Test nicht läuft, weil ein Werkzeug fehlt (z. B. .NET SDK), sage es und tue nicht so, als sei es grün. Behaupte nie Verhalten, das du nicht ausgeführt hast.

## Reihenfolge der Meilensteine

Siehe `docs/roadmap.md`. Meilensteine nicht überspringen. M1 (Engine-Core) kommt bewusst vor M2 (Unity-Frontend), weil sich erst am laufenden Core zeigt, was die Engine-API braucht.
