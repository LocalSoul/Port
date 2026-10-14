package io.github.localsoul.port.ir.refs;

import io.github.localsoul.port.ir.SourceSpan;
import io.github.localsoul.port.ir.ast.IrFile;
import io.github.localsoul.port.ir.atom.MemberRef;
import io.github.localsoul.port.ir.atom.TypeRef;
import java.util.ArrayList;
import java.util.List;

/**
 * Sammelt alle Typ- und Member-Referenzen einer Datei in Eingabereihenfolge, jeweils mit umgebendem
 * Symbol und sichtbaren generischen Typparametern. Grundlage für den Validator (IR002/IR003), den
 * Index (M5) und den Lint (M6).
 */
public final class References extends NodeWalker {

  private final List<Reference> references = new ArrayList<>();

  private References() {}

  /**
   * Sammelt die Referenzen einer Datei.
   *
   * @param file gelesene IR-Datei
   * @return Referenzen in Eingabereihenfolge
   */
  public static List<Reference> collect(IrFile file) {
    References collector = new References();
    collector.walk(file);
    return collector.references;
  }

  @Override
  protected void onType(TypeRef type, SourceSpan span) {
    references.add(new Reference(type, null, span, symbol(), genericScope()));
  }

  @Override
  protected void onMember(MemberRef ref, SourceSpan span) {
    references.add(new Reference(null, ref, span, symbol(), genericScope()));
  }
}
