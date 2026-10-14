package io.github.localsoul.launcher.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.localsoul.launcher.LauncherException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PlatformCheckTest {

  @TempDir Path module;

  @Test
  void builtModuleIsAccepted() throws Exception {
    Files.createDirectories(module.resolve("src/main/java"));
    Files.writeString(module.resolve("src/main/java/A.java"), "class A {}");
    Files.setLastModifiedTime(module.resolve("src/main/java/A.java"), FileTime.fromMillis(1_000));
    Files.createDirectories(module.resolve("target/classes"));
    Files.writeString(module.resolve("target/classes/A.class"), "x");
    Files.setLastModifiedTime(module.resolve("target/classes/A.class"), FileTime.fromMillis(2_000));
    assertEquals(List.of(module.resolve("target/classes")), PlatformCheck.classes(List.of(module)));
  }

  @Test
  void missingOrStaleClassesGiveExit8() throws Exception {
    LauncherException missing =
        assertThrows(LauncherException.class, () -> PlatformCheck.classes(List.of(module)));
    assertEquals(LauncherException.DEPENDENCY, missing.exitCode());
    assertEquals("./mvnw -q install", missing.hint());
    Files.createDirectories(module.resolve("target/classes"));
    Files.writeString(module.resolve("target/classes/A.class"), "x");
    Files.setLastModifiedTime(module.resolve("target/classes/A.class"), FileTime.fromMillis(1_000));
    Files.createDirectories(module.resolve("src/main/java"));
    Files.writeString(module.resolve("src/main/java/A.java"), "class A {}");
    Files.setLastModifiedTime(module.resolve("src/main/java/A.java"), FileTime.fromMillis(5_000));
    LauncherException stale =
        assertThrows(LauncherException.class, () -> PlatformCheck.classes(List.of(module)));
    assertEquals(LauncherException.DEPENDENCY, stale.exitCode());
  }
}
