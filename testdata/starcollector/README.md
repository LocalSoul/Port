# StarCollector

Eigenes, selbst kompiliertes Unity-Spiel des Nutzers (Mono-Build), als reale Testeingabe für das Unity-Frontend (M2.6). Kein kommerzielles Spiel.

- `Assembly-CSharp.dll`: Spiel-Assembly aus dem Linux-Build (`StarCollector_Data/Managed/`). Die Windows-Assembly ist ein eigener Build und unterscheidet sich, maßgeblich ist diese.
- **Nicht** im Repo: der vollständige Build und die Unity-DLLs (`UnityEngine.*.dll`), weil sie groß und proprietär sind. Liegt der Build lokal unter `testdata/Builds/Linux/StarCollector_Data/Managed/`, nutzt der Probelauf diese DLLs zur Typauflösung; sonst bleiben Unity-Typen unaufgelöst.
- Keine Golden Files: Das Spiel kann sich ändern. Der Probelauf schreibt einen Abdeckungsbericht.
