package io.github.localsoul.port.ir.validate;

import io.github.localsoul.port.ir.SourceSpan;
import java.util.ArrayList;
import java.util.List;

/** Sammelt Diagnosen der Regeln. */
final class DiagnosticSink {

  private final List<Diagnostic> diagnostics = new ArrayList<>();

  void error(String id, Reference reference, String message, String hint) {
    add(id, Severity.ERROR, reference.symbol(), message, reference.span(), hint);
  }

  void info(String id, Reference reference, String message, String hint) {
    add(id, Severity.INFO, reference.symbol(), message, reference.span(), hint);
  }

  void add(
      String id,
      Severity severity,
      String symbol,
      String message,
      SourceSpan location,
      String hint) {
    diagnostics.add(new Diagnostic(id, severity, symbol, message, location, hint));
  }

  /** Liefert alle Diagnosen, sortiert nach {@link Diagnostic#ORDER}. */
  List<Diagnostic> sorted() {
    List<Diagnostic> result = new ArrayList<>(diagnostics);
    result.sort(Diagnostic.ORDER);
    return List.copyOf(result);
  }
}
