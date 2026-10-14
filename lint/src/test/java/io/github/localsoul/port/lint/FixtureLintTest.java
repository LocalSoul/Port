package io.github.localsoul.port.lint;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.localsoul.port.ir.reader.IrReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/** Lint über die IR-Fixtures mit den echten Mappings des Repos. */
class FixtureLintTest {

  private static final Path ROOT = Path.of("..").toAbsolutePath().normalize();

  static Coverage repositoryCoverage() throws Exception {
    Coverage coverage = new Coverage();
    Path module = ROOT.resolve("engines/unity/adapter-unity/src/main/resources");
    String metadata =
        Files.readString(module.resolve("adapter-metadata.json"), StandardCharsets.UTF_8);
    Matcher matcher =
        Pattern.compile("\"id\"\\s*:\\s*\"([^\"]+)\"\\s*,\\s*\"status\"\\s*:\\s*\"(full|stub)\"")
            .matcher(metadata);
    while (matcher.find()) {
      coverage.put(matcher.group(1), matcher.group(2));
    }
    for (Path mapping :
        List.of(
            module.resolve("mapping.tsv"),
            ROOT.resolve("runtime-system/src/main/resources/mapping.tsv"))) {
      for (String line : Files.readAllLines(mapping, StandardCharsets.UTF_8)) {
        if (!line.isBlank() && !line.startsWith("#")) {
          coverage.put(line.substring(0, line.indexOf('\t')), Coverage.FULL);
        }
      }
    }
    return coverage;
  }

  @Test
  void fixturesReportAdapterGapsAndOpenHandwork() throws Exception {
    List<IrSource> sources = new ArrayList<>();
    Path dir = ROOT.resolve("testdata/golden/ir/unity");
    List<Path> paths = new ArrayList<>();
    try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.ir")) {
      for (Path path : stream) {
        paths.add(path);
      }
    }
    paths.sort(null);
    for (Path path : paths) {
      String name = "ir/" + path.getFileName();
      String text = Files.readString(path, StandardCharsets.UTF_8);
      sources.add(new IrSource(name, text, new IrReader().read(text, name), null));
    }
    List<String> findings = new ArrayList<>();
    for (LintFinding finding :
        Linter.lint(new LintInput(sources, List.of(), repositoryCoverage()), LintConfig.DEFAULT)) {
      findings.add(finding.id() + " " + finding.symbol() + " " + finding.message());
    }
    assertTrue(
        findings.contains(
            "SH001 Counter::Update# genutzte API fehlt im Adapter: UnityEngine.Input::GetKey#string"),
        findings.toString());
    assertTrue(
        findings.contains("MN003 Edges::producer @manual ohne Override in manual/"),
        findings.toString());
    assertTrue(
        findings.contains("MN003 UnsafeBits::First#i32[] @manual ohne Override in manual/"),
        findings.toString());
    for (String finding : findings) {
      assertTrue(finding.startsWith("SH") || finding.startsWith("MN003"), "unerwartet: " + finding);
    }
    assertEquals(findings, findings.stream().distinct().toList());
  }
}
