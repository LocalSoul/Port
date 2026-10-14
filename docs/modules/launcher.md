# Modul `launcher`

## Zweck
Zusammenstellen und Starten: Der Launcher findet Spiele, Engines und Provider im Repo, prüft ein Profil auf Kompatibilität und startet das Ergebnis. Konzept und Profilformat: `docs/projects.md`.

## Aufbau
```
launcher/
  launcher-core/    Katalog, Profile, Kompatibilitätsprüfung, Prozessstart (reine Java-Bibliothek)
  launcher-boot/    Startklasse der Spiel-JVM: Core + Adapter (EngineBinding) + Spielklassen (M4a)
  launcher-ui/      ab M12a, Oberfläche (Technik laut ADR-021)
```
`port-cli` bindet `launcher-core` ein und stellt `port catalog` und `port launch` bereit. Die Oberfläche nutzt ausschließlich `launcher-core`.

## Abhängigkeiten
- `launcher-core`: nur JDK, eine TOML-Bibliothek, `engine-api` (nur für die Metadatentypen `SubsystemInfo`). Kein `engine-core`, kein Adapter, kein Spielcode im Klassenpfad des Launchers.
- `launcher-boot`: `engine-api` und `engine-core`; Adapter und Spiel kommen erst zur Laufzeit auf den Klassenpfad (Enforcer, ArchUnit). Einzige Reflection der Plattform: Laden der Spielklassen aus `[run] spawn` per Namen.
- Gestartet wird immer in einer **eigenen JVM**, damit Spiel und Engine den Launcher nicht beeinflussen.

## Aufgaben
- **Katalog:** durchsucht `games/*/port.toml`, `engines/*/port.toml`, `engines/*/providers/*`. Ergebnis ist deterministisch sortiert.
- **Profile:** lesen, schreiben, validieren (`profiles/*.toml`).
- **Kompatibilitätsprüfung:** Adapter-Name und API-Version, Core-API-Version, ein Provider je Subsystem-Art, Provider-Verträge, Pflichtpaare und Konflikte.
- **Bau-Aufträge:** erkennt veraltete Artefakte (Hash der Eingaben) und ruft `port build` bzw. Maven auf.
- **Klassenpfad:** setzt ihn aus Artefakten zusammen und startet den Prozess.

## Stand M4a
- Pakete von `launcher-core`: `manifest` (Spiel- und Engine-Manifest, `TomlFile`/`TomlText`, `VersionRange`, `CoreApi`), `catalog` (`Catalog`, `Coverage` aus `adapter-metadata.json`), `profile` (`Profile`, `ProfileStore`, `Compatibility`), `launch` (`BuildState`, `PlatformCheck`, `Launcher`, Schnittstelle `GameBuilder`, umgesetzt von `port-cli`).
- `launch run`: Profil prüfen; Spiel-Hash über `ir/`, `manual/`, `port.toml` und die Mappings (`.port/build.hash`), bei Abweichung `gen` + `build`; Engine-Modul und Plattform (`engine-api`, `engine-core`, `runtime-system`, `launcher-boot`) müssen gebaut und nicht älter als ihre Quellen sein (sonst Exit 8, kein Maven-Aufruf); Klassenpfad: Spiel `.port/classes`, Engine-Modul, Plattform; `java -cp … io.github.localsoul.launcher.boot.BootMain --adapter <a> --frames <n> <args> -- <Klassen>`.
- `BootMain`: `CoreEngine` mit `ScriptedInput` (`--axis Name=Wert` ab Frame 0), Adapter per `ServiceLoader<EngineBinding>`, je Spielklasse ein Objekt, N Frames, Welt-Dump auf stdout. Exit 0, 1 (Fehler im Spiel, z. B. `PORT-TODO`), 2 (Argumente, kein Adapter), 3 (Klasse fehlt).
- Provider (ADR-015) werden nur aufgelistet; Auswahl und Prüfung kommen mit M12.

## Tests
- Katalog gegen ein synthetisches Mini-Repo (`@TempDir`) mit einem Spiel und zwei Engines.
- Kompatibilitätsfälle: passend, falsche API-Version, doppelter Provider, fehlender Provider, Konfliktpaar.
- Prozessstart gegen ein triviales Test-Spiel (Exit-Code und Ausgabe werden durchgereicht).
- Golden Files für `--json`-Ausgaben.

## Nicht-Ziele
- Keine Spiellogik, keine Engine-Logik.
- Kein Download oder Installieren von Spielen.
- Kein Paketmanager für Engines (alles liegt im Repo).
