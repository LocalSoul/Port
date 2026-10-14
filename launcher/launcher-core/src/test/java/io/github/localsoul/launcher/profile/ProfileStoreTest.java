package io.github.localsoul.launcher.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import io.github.localsoul.launcher.LauncherException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ProfileStoreTest {

  @TempDir Path root;

  @Test
  void writtenProfileReadsBackAndIsListed() throws Exception {
    Profile profile =
        new Profile(
            "demo-unity",
            "games/demo",
            "engines/unity",
            "engine-core",
            true,
            60,
            List.of("Mover"),
            List.of("--axis", "Horizontal=1"));
    Files.createDirectories(root.resolve("profiles"));
    Files.writeString(root.resolve(profile.path()), profile.toToml());
    Files.writeString(root.resolve("profiles/b.toml"), Profile.create("b", "g", "e").toToml());
    assertEquals(profile, ProfileStore.read(root, "demo-unity"));
    assertEquals(List.of("b", "demo-unity"), ProfileStore.names(root));
  }

  @Test
  void errorsHaveLocationAndNextStep() throws Exception {
    LauncherException missing =
        assertThrows(LauncherException.class, () -> ProfileStore.read(root, "fehlt"));
    assertEquals(LauncherException.INVALID, missing.exitCode());
    assertEquals(
        "port launch list oder port launch new fehlt --game <spiel> --engine <engine>",
        missing.hint());
    assertThrows(LauncherException.class, () -> ProfileStore.read(root, "Böse"));
    LauncherException frames =
        assertThrows(
            LauncherException.class,
            () ->
                ProfileStore.parse(
                    "[profile]\nname = \"a\"\ngame = \"g\"\n[engine]\nadapter = \"e\"\n[run]\nframes = -1\n",
                    "profiles/a.toml"));
    assertEquals("profiles/a.toml:7:1", frames.location());
    assertThrows(
        LauncherException.class,
        () ->
            ProfileStore.parse(
                "[profile]\nname = \"a\"\ngame = \"g\"\n[engine]\nadapter = \"e\"\n[run]\nheadless = 1\n",
                "x"));
  }
}
