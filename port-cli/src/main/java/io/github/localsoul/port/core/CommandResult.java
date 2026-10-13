package io.github.localsoul.port.core;

import io.github.localsoul.port.json.JsonObject;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Ergebnis eines Befehls; der Renderer macht daraus Text oder die JSON-Hülle, {@link ExitCodes} den
 * Exit-Code.
 *
 * @param command Befehlsname, z. B. {@code init}
 * @param data befehlsspezifische Daten ({@code docs/cli-contract.md})
 * @param diagnostics Meldungen, sortiert nach Ort und ID
 * @param failure Fehlerklasse, wenn der Befehl abgebrochen hat, sonst {@code null}
 */
public record CommandResult(
    String command, JsonObject data, List<Diagnostic> diagnostics, ExitCode failure) {

  private static final Comparator<Diagnostic> ORDER =
      Comparator.comparing(
              (Diagnostic d) -> d.location() == null ? "" : d.location(), LocationOrder.INSTANCE)
          .thenComparing(Diagnostic::id)
          .thenComparing(Diagnostic::message);

  /** Sortiert die Diagnosen (Vertrag: nach {@code location}, dann {@code id}). */
  public CommandResult {
    List<Diagnostic> sorted = new ArrayList<>(diagnostics);
    sorted.sort(ORDER);
    diagnostics = List.copyOf(sorted);
  }

  /**
   * Erfolgreiches Ergebnis.
   *
   * @param command Befehl
   * @param data Daten
   * @param diagnostics Warnungen und Hinweise
   * @return Ergebnis
   */
  public static CommandResult ok(String command, JsonObject data, List<Diagnostic> diagnostics) {
    return new CommandResult(command, data, diagnostics, null);
  }

  /**
   * Abgebrochenes Ergebnis.
   *
   * @param command Befehl
   * @param data bis zum Abbruch gesammelte Daten
   * @param diagnostics Meldungen
   * @param failure Fehlerklasse
   * @return Ergebnis
   */
  public static CommandResult failed(
      String command, JsonObject data, List<Diagnostic> diagnostics, ExitCode failure) {
    return new CommandResult(command, data, diagnostics, failure);
  }
}
