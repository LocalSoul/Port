package io.github.localsoul.launcher.catalog;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CatalogTest {

  @TempDir Path root;

  private static List<String> names(List<CatalogEntry> entries) {
    List<String> names = new ArrayList<>();
    for (CatalogEntry entry : entries) {
      names.add(entry.kind() + ":" + entry.name() + "@" + entry.path());
    }
    return names;
  }

  @Test
  void findsProjectsAndProvidersSortedByKindAndName() throws Exception {
    MiniRepo.create(root);
    Catalog catalog = Catalog.scan(root);
    assertEquals(
        List.of(
            "game:demo@games/demo",
            "engine:godot@engines/godot",
            "engine:unity@engines/unity",
            "provider:input-godot-like@engines/godot/providers/input-godot-like"),
        names(catalog.entries()));
    assertEquals("unity", catalog.engine("unity").name());
    assertNull(catalog.game("unity"));
  }

  @Test
  void brokenProjectBecomesProblemNotFailure() throws Exception {
    MiniRepo.create(root);
    Catalog catalog = Catalog.scan(root);
    assertEquals(1, catalog.problems().size());
    Problem problem = catalog.problems().get(0);
    assertEquals("Pflichtfeld fehlt: project.name", problem.message());
    assertEquals("games/kaputt/port.toml", problem.location());
  }

  @Test
  void readsCoverageFromAdapterMetadata() throws Exception {
    MiniRepo.create(root);
    assertEquals(new Coverage(2, 1), Coverage.of(root.resolve("engines/unity/adapter-unity")));
    assertNull(Coverage.of(root.resolve("engines/godot/adapter-godot")));
  }

  @Test
  void emptyRepoGivesEmptyCatalog() throws Exception {
    assertEquals(List.of(), Catalog.scan(root).entries());
  }
}
