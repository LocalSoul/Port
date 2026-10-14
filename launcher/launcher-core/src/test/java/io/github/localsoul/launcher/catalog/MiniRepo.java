package io.github.localsoul.launcher.catalog;

import io.github.localsoul.launcher.manifest.EngineManifest;
import io.github.localsoul.launcher.manifest.GameManifest;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

/** Synthetisches Mini-Monorepo: ein Spiel, zwei Engines (eine mit Provider), ein defektes Spiel. */
public final class MiniRepo {

  private MiniRepo() {}

  /**
   * Legt das Repo an.
   *
   * @param root leerer Ordner
   * @throws IOException bei Schreibfehlern
   */
  public static void create(Path root) throws IOException {
    write(root.resolve("pom.xml"), "<project><artifactId>recomp-parent</artifactId></project>");
    GameManifest game =
        new GameManifest("demo", "unity", "input/a.dll", null, "unity", ">=2022.3", List.of());
    write(root.resolve("games/demo/port.toml"), game.toToml());
    write(root.resolve("games/kaputt/port.toml"), "[project]\nkind = \"game\"\n");
    EngineManifest unity =
        new EngineManifest(
            "unity", "unity", "2022.3", ">=0.1 <0.2", List.of(), "adapter-unity", "a.Main");
    write(root.resolve("engines/unity/port.toml"), unity.toToml());
    write(
        root.resolve("engines/unity/adapter-unity/" + Coverage.METADATA),
        "{ \"symbols\": [ { \"id\": \"A\", \"status\": \"full\" }, { \"id\": \"B\", \"status\": \"stub\" },"
            + " { \"id\": \"C\", \"status\": \"full\" } ] }");
    EngineManifest godot =
        new EngineManifest(
            "godot",
            "godot",
            "4.2",
            ">=0.1 <0.2",
            List.of("input-godot-like"),
            "adapter-godot",
            "g.Main");
    write(root.resolve("engines/godot/port.toml"), godot.toToml());
    Files.createDirectories(root.resolve("engines/godot/providers/input-godot-like"));
  }

  private static void write(Path file, String text) throws IOException {
    Files.createDirectories(file.getParent());
    Files.writeString(file, text);
  }
}
