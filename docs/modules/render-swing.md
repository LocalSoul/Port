# Modul `render-swing`

## Zweck
Erstes Render-Backend (ADR-014): setzt die neutrale Schnittstelle `RenderBackend` aus `engine-api` mit Swing/Java2D um. Der Core-Renderer (`SpriteRenderSystem`) übersetzt die Welt in Zeichenbefehle, dieses Modul führt sie aus. Weitere Backends (z. B. LWJGL) kommen als eigene Module, ohne Änderung an Core, Adapter oder Spiel.

## Abhängigkeiten
Nur `engine-api` und das JDK (`java.desktop`). Kein `engine-core`, kein Adapter (Enforcer, ArchUnit `ArchitectureTest`).

## Inhalt
| Klasse | Aufgabe |
|---|---|
| `OffscreenBackend` | zeichnet in ein `BufferedImage`, läuft ohne Bildschirm; `image()`, `frames()`, `writePng(Path)`; keine Tasten, nie geschlossen |
| `WindowBackend` | `JFrame` mit `BufferStrategy`, zeichnet in ein Bild und zeigt es bei `present`; Tasten über `KeyListener`, Fenster-Schließen setzt `closed()` |
| `Java2DCanvas` | gemeinsame Zeichenlogik: Hintergrund, Texturen mit `Affine2D` (bilinear oder nächster Nachbar je nach `Texture.smooth`), Tönung durch Multiplikation, Rechtecke mit Rand, Text (`SansSerif`, fett, Anker 0–8 wie Unitys `TextAnchor`) mit Kantenglättung |
| `KeyNames` | AWT-Tastencodes → neutrale Namen aus `KeyEvent` (`LEFT`, `A`, `ESCAPE`, …); unbekannte Tasten entfallen |

Texturen werden je Instanz und Tönung einmal in Bilder umgewandelt und zwischengespeichert (`WeakHashMap`, da Komponenten unveränderlich sind, bedeutet eine neue Instanz neue Pixel).

## Verhalten
- Pixelraum: Ursprung oben links, y nach unten. Die Abbildung aus Weltkoordinaten berechnet der Core.
- Schriftbild hängt von den installierten Schriften ab. Bildvergleiche in Tests deshalb nur an Farben und gleichen Bereichen desselben Laufs, nicht gegen eingecheckte Bilder.
- Fenster: Ohne Display wirft `WindowBackend` beim Erzeugen (`HeadlessException`); `launcher-boot` meldet das mit Exit 1.

## Tests
- `OffscreenBackendTest` (headless): skalierte und verschobene Textur, Tönung, Rechtecke und Text, PNG-Ausgabe, Tastennamen.
- Das Fenster wird von Hand geprüft (`port launch run starcollector-unity`).

## Nicht-Ziele
- Kein Shader, kein GPU-Pfad, keine 3D-Ausgabe. Kein eigener Spielschleifen-Takt (macht `launcher-boot`).
