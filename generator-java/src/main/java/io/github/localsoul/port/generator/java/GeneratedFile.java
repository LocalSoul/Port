package io.github.localsoul.port.generator.java;

/**
 * Eine erzeugte Java-Datei.
 *
 * @param path relativer Pfad, z. B. {@code gen/testgame/MoverBase.java}
 * @param content Quelltext, endet mit genau einem {@code \n}
 */
public record GeneratedFile(String path, String content) {}
