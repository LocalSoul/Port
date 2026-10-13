package io.github.localsoul.port.core;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;

/**
 * Dateizugriffe der CLI: atomar (temporäre Datei, dann umbenennen) und idempotent (unveränderter
 * Inhalt wird nicht neu geschrieben, Zeitstempel bleiben).
 */
public final class ProjectFiles {

  private ProjectFiles() {}

  /**
   * Schreibt Text als UTF-8, wenn er sich vom Inhalt der Datei unterscheidet.
   *
   * @param file Ziel
   * @param text Inhalt
   * @return {@code true}, wenn geschrieben wurde
   * @throws IOException bei Schreibfehlern
   */
  public static boolean writeAtomic(Path file, String text) throws IOException {
    return writeAtomic(file, text.getBytes(StandardCharsets.UTF_8));
  }

  /**
   * Schreibt Bytes, wenn sie sich vom Inhalt der Datei unterscheiden.
   *
   * @param file Ziel
   * @param content Inhalt
   * @return {@code true}, wenn geschrieben wurde
   * @throws IOException bei Schreibfehlern
   */
  public static boolean writeAtomic(Path file, byte[] content) throws IOException {
    if (Files.isRegularFile(file) && Arrays.equals(Files.readAllBytes(file), content)) {
      return false;
    }
    Path parent = file.toAbsolutePath().getParent();
    Files.createDirectories(parent);
    Path temp = Files.createTempFile(parent, "." + file.getFileName(), ".tmp");
    try {
      Files.write(temp, content);
      move(temp, file);
    } finally {
      Files.deleteIfExists(temp);
    }
    return true;
  }

  private static void move(Path temp, Path file) throws IOException {
    try {
      Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
    } catch (AtomicMoveNotSupportedException e) {
      Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
    }
  }

  /**
   * SHA-256 einer Datei.
   *
   * @param file Datei
   * @return Hash als 64 Hex-Zeichen in Kleinbuchstaben
   * @throws IOException bei Lesefehlern
   */
  public static String sha256(Path file) throws IOException {
    MessageDigest digest;
    try {
      digest = MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 fehlt in der JVM", e);
    }
    byte[] buffer = new byte[64 * 1024];
    try (InputStream in = Files.newInputStream(file)) {
      int read = in.read(buffer);
      while (read >= 0) {
        digest.update(buffer, 0, read);
        read = in.read(buffer);
      }
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  /**
   * Pfad relativ zu {@code base} mit {@code /} als Trenner (für Ausgaben).
   *
   * @param base Basis
   * @param path Pfad unterhalb der Basis
   * @return relativer Pfad
   */
  public static String relative(Path base, Path path) {
    return base.relativize(path).toString().replace('\\', '/');
  }
}
