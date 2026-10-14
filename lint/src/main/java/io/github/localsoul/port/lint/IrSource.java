package io.github.localsoul.port.lint;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.ast.IrFile;

/**
 * Eine IR-Datei als Eingabe des Lint: gelesen oder mit Syntaxfehler (IR001).
 *
 * @param path Pfad relativ zum Projekt, z. B. {@code ir/Mover.ir}
 * @param text Inhalt (für Hashes der Symbole)
 * @param file gelesene Datei oder {@code null} bei Syntaxfehler
 * @param error Syntaxfehler oder {@code null}
 */
public record IrSource(String path, String text, IrFile file, IrSyntaxException error) {}
