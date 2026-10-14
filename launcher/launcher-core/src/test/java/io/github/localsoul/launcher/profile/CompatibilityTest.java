package io.github.localsoul.launcher.profile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.launcher.catalog.Catalog;
import io.github.localsoul.launcher.catalog.MiniRepo;
import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.manifest.GameManifest;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Kompatibilitätsfälle aus der Abnahme M4a.3. */
class CompatibilityTest {

  @TempDir Path root;

  private Catalog catalog;

  @BeforeEach
  void createRepo() throws Exception {
    MiniRepo.create(root);
    GameManifest newer =
        new GameManifest("neu", "unity", "input/a.dll", null, "unity", ">=2023.1", List.of());
    Files.createDirectories(root.resolve("games/neu"));
    Files.writeString(root.resolve("games/neu/port.toml"), newer.toToml());
    EngineManifest future =
        new EngineManifest(
            "zukunft", "unity", "2022.3", ">=9.0", List.of(), "adapter-unity", "a.Main");
    Files.createDirectories(root.resolve("engines/zukunft"));
    Files.writeString(root.resolve("engines/zukunft/port.toml"), future.toToml());
    catalog = Catalog.scan(root);
  }

  private static List<String> failed(List<Finding> findings) {
    List<String> checks = new ArrayList<>();
    for (Finding finding : findings) {
      if (!finding.ok()) {
        checks.add(finding.check());
        assertTrue(finding.hint() != null, finding.toString());
      }
    }
    return checks;
  }

  private List<Finding> check(String game, String engine) {
    return Compatibility.check(Profile.create("p", game, engine), catalog);
  }

  @Test
  void matchingProfile() {
    List<Finding> findings = check("games/demo", "engines/unity");
    assertTrue(Compatibility.compatible(findings), findings.toString());
    assertEquals(6, findings.size());
  }

  @Test
  void wrongAdapter() {
    assertEquals(List.of("adapter", "api"), failed(check("games/demo", "engines/godot")));
  }

  @Test
  void apiOutsideRange() {
    assertEquals(List.of("api"), failed(check("games/neu", "engines/unity")));
  }

  @Test
  void coreApiOutsideRange() {
    assertEquals(List.of("core-api"), failed(check("games/demo", "engines/zukunft")));
  }

  @Test
  void missingGameOrEngine() {
    assertEquals(List.of("game"), failed(check("games/fehlt", "engines/unity")));
    assertEquals(List.of("engine"), failed(check("games/demo", "engines/fehlt")));
    assertFalse(Compatibility.compatible(check("games/fehlt", "engines/fehlt")));
  }

  @Test
  void unknownCore() {
    Profile profile =
        new Profile(
            "p", "games/demo", "engines/unity", "anderer-core", true, 1, List.of(), List.of());
    assertEquals(List.of("core"), failed(Compatibility.check(profile, catalog)));
  }
}
