@AGENTS.md

# Zusätze für Claude Code

- Nutze Plan Mode für jede Aufgabe, die mehr als ein Modul berührt, und stimme den Plan mit dem Nutzer ab, bevor du Code schreibst.
- Arbeite eine Aufgabe aus `tasks/` pro Branch ab (`task/<id>-kurzname`).
- Halte die Task-Liste aktuell und markiere Schritte erst als erledigt, wenn die Tests dazu grün sind.
- Lies Dateien gezielt (mit Offset und Limit), statt ganze große Dateien zu laden.
- Für breite Codesuche (viele Dateien) kannst du einen Explore-Subagent nutzen, für Änderungen arbeitest du selbst.
