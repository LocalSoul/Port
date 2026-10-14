package io.github.localsoul.launcher.launch;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;

/**
 * Hash der Eingaben eines Spiel-Baus ({@code ir/}, {@code manual/}, {@code port.toml} und die
 * Mappings der Engine) in {@code .port/build.hash}. Weicht er ab oder fehlen die Klassen, ist das
 * Spiel veraltet.
 */
public final class BuildState {

  /** Datei mit dem Hash des letzten erfolgreichen Baus. */
  public static final String FILE = ".port/build.hash";

  /** Klassen des Spiels. */
  public static final String CLASSES = ".port/classes";

  private BuildState() {}

  /**
   * Berechnet den Hash der Eingaben.
   *
   * @param gameDir Spielordner
   * @param extraFiles weitere Eingaben (Mappings)
   * @return SHA-256 als Hex
   * @throws IOException bei Lesefehlern
   */
  public static String hash(Path gameDir, List<Path> extraFiles) throws IOException {
    List<Path> files = new ArrayList<>();
    collect(gameDir.resolve("ir"), files);
    collect(gameDir.resolve("manual"), files);
    files.add(gameDir.resolve("port.toml"));
    files.sort(null);
    MessageDigest digest = sha256();
    for (Path file : files) {
      update(digest, gameDir.relativize(file).toString().replace('\\', '/'), file);
    }
    for (Path file : extraFiles) {
      update(digest, file.getFileName().toString(), file);
    }
    return HexFormat.of().formatHex(digest.digest());
  }

  /**
   * Prüft, ob der letzte Bau zu den Eingaben passt.
   *
   * @param gameDir Spielordner
   * @param hash aktueller Hash
   * @return {@code true}, wenn Hash gleich und Klassen vorhanden sind
   */
  public static boolean upToDate(Path gameDir, String hash) {
    Path file = gameDir.resolve(FILE);
    if (!Files.isRegularFile(file) || !Files.isDirectory(gameDir.resolve(CLASSES))) {
      return false;
    }
    try {
      return Files.readString(file, StandardCharsets.UTF_8).strip().equals(hash);
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Hält einen erfolgreichen Bau fest.
   *
   * @param gameDir Spielordner
   * @param hash Hash der Eingaben
   * @throws IOException bei Schreibfehlern
   */
  public static void save(Path gameDir, String hash) throws IOException {
    Path file = gameDir.resolve(FILE);
    Files.createDirectories(file.getParent());
    Files.writeString(file, hash + "\n", StandardCharsets.UTF_8);
  }

  private static void update(MessageDigest digest, String name, Path file) throws IOException {
    digest.update(name.getBytes(StandardCharsets.UTF_8));
    digest.update((byte) 0);
    if (Files.isRegularFile(file)) {
      try (InputStream in = Files.newInputStream(file)) {
        digest.update(in.readAllBytes());
      }
    }
    digest.update((byte) 0);
  }

  private static void collect(Path dir, List<Path> files) throws IOException {
    if (!Files.isDirectory(dir)) {
      return;
    }
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
      for (Path path : stream) {
        if (Files.isDirectory(path)) {
          collect(path, files);
        } else {
          files.add(path);
        }
      }
    }
  }

  private static MessageDigest sha256() {
    try {
      return MessageDigest.getInstance("SHA-256");
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException("SHA-256 fehlt in der JVM", e);
    }
  }
}
