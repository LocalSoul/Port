package io.github.localsoul.port.lint;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * MN001–MN004 über die Markierungen {@code // port:override} in {@code manual/} (ADR-027): MN001
 * Symbol fehlt in der IR, MN002 Hash weicht ab (IR seit dem Override geändert), MN003
 * Handarbeitsstelle ohne Markierung, MN004 Markierung auf ein Symbol ohne Handarbeit.
 */
final class ManualRule implements Rule {

  /** Mindestlänge des Hash-Präfixes in einer Markierung. */
  static final int MIN_HASH = 12;

  @Override
  public void check(LintInput input, ProjectModel model, List<LintFinding> findings) {
    Set<String> covered = new TreeSet<>();
    for (ManualOverride override : input.overrides()) {
      covered.add(override.symbol());
      String hash = model.hashes.get(override.symbol());
      if (hash == null) {
        findings.add(
            new LintFinding(
                "MN001",
                Severity.ERROR,
                override.symbol(),
                "Override zeigt auf ein Symbol, das es in der IR nicht mehr gibt",
                override.location(),
                "port find " + shortName(override.symbol())));
      } else if (override.hash().length() < MIN_HASH || !hash.startsWith(override.hash())) {
        findings.add(
            new LintFinding(
                "MN002",
                Severity.ERROR,
                override.symbol(),
                "IR des Symbols hat sich seit dem Override geändert",
                override.location(),
                "Handarbeit prüfen, dann ir=" + hash.substring(0, MIN_HASH) + " eintragen"));
      } else if (!model.isManual(override.symbol())) {
        findings.add(
            new LintFinding(
                "MN004",
                Severity.INFO,
                override.symbol(),
                "Override auf ein Symbol ohne @manual (raw/:manual)",
                override.location(),
                null));
      }
    }
    for (Map.Entry<String, String> spot : model.manualSpots.entrySet()) {
      if (covered.contains(spot.getKey())) {
        continue;
      }
      String hash = model.hashes.get(spot.getKey());
      findings.add(
          new LintFinding(
              "MN003",
              Severity.WARN,
              spot.getKey(),
              "@manual ohne Override in manual/",
              spot.getValue(),
              "in manual/ umsetzen mit // port:override "
                  + spot.getKey()
                  + (hash == null ? "" : " ir=" + hash.substring(0, MIN_HASH))));
    }
  }

  private static String shortName(String symbol) {
    String name = symbol.contains("::") ? symbol.substring(symbol.indexOf("::") + 2) : symbol;
    int hash = name.indexOf('#');
    return hash < 0 ? name : name.substring(0, hash);
  }
}
