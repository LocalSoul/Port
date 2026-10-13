package io.github.localsoul.port.core;

import static org.junit.jupiter.api.Assertions.assertEquals;

import io.github.localsoul.port.cli.Golden;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** {@link ExitCode} entspricht der Tabelle „Exit-Codes“ in {@code docs/cli-contract.md}. */
class ExitCodeContractTest {

  @Test
  void enumMatchesContractTable() throws Exception {
    String contract =
        Files.readString(Golden.ROOT.resolve("docs/cli-contract.md"), StandardCharsets.UTF_8);
    String table = contract.substring(contract.indexOf("## Exit-Codes"));
    table = table.substring(0, table.indexOf("\n## ", 5));
    List<Integer> documented = new ArrayList<>();
    for (String line : table.split("\n")) {
      if (line.matches("\\| \\d+ \\|.*")) {
        documented.add(Integer.parseInt(line.substring(2, line.indexOf(" |", 2)).trim()));
      }
    }
    List<Integer> implemented = new ArrayList<>();
    for (ExitCode code : ExitCode.values()) {
      implemented.add(code.code());
    }
    assertEquals(documented, implemented);
  }

  @Test
  void unknownNumbersMapToGeneral() {
    assertEquals(ExitCode.TRANSLATOR, ExitCode.of(4));
    assertEquals(ExitCode.GENERAL, ExitCode.of(99));
  }
}
