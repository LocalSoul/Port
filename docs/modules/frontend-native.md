# Modul `frontend-native` (ab M10b, ADR-023)

## Zweck
Native Binärdateien (x86-64, später ARM64) disassemblieren und in die IR heben. Gilt für Engine-Recomps (Kerne, Bibliotheken) und Game-Recomps (native Plugins, IL2CPP-Spiele). Ziel: Nativer Code wird, wo immer möglich, gehoben und nicht von Hand nachgebaut.

## Abhängigkeiten
`ir`. Disassembler/Heber läuft als externer Prozess (Werkzeugwahl: eigene Entscheidung vor M10b).

## Ablauf
```
Binary -> [Disassembler/Heber] -> Zwischenform (JSON) -> [Java-Wrapper] -> IR (strukturiert oder native-Dialekt)
```
1. **Extrahieren:** Funktionen, Symbole, Debuginfos, Metadaten, Importe/Exporte. Hash der Eingabe wird geprüft.
2. **Heben:** pro Funktion zuerst Struktur rückgewinnen (Kontrollfluss, Typen, Aufrufkonventionen), Ergebnis in die normale IR. Gelingt das nicht, wird der niedrigere `native`-Dialekt verwendet.
3. **Metadaten nutzen:** Namen, Typen und Signaturen aus Symbolen und aus Spiel-Metadaten (z. B. IL2CPP) übernehmen. Zuordnung zu stabilen Symbol-IDs wie bei verwaltetem Code.
4. **Markieren:** Nicht gehobenes wird `:manual` mit Grund (Kategorie), nie still verworfen.

## Ergebnis pro Funktion
Jede Funktion bekommt einen Status: `lifted` (strukturierte IR), `lifted-low` (native-Dialekt), `manual` (Handarbeit mit Konformitätstest). Der Status steht im Index und in `port status`.

## Tests
- Synthetische native Fixtures (kleine C-Funktionen, im Repo kompiliert): Heben, Rückübersetzen nach Java, Differenztest gegen die native Funktion.
- Golden Files für die IR.
- Jede Lint-Regel `NT*` hat einen Test.

## Nicht-Ziele
- Kein Entpacken oder Umgehen von Schutzmaßnahmen.
- Keine vollständige Dekompilierung beliebiger Programme: nur, was Recomp-Projekte tatsächlich brauchen.
