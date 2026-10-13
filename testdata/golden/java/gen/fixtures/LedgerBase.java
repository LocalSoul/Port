package gen.fixtures;

import io.github.localsoul.runtime.system.InvalidOperationException;
import io.github.localsoul.runtime.system.NullReferenceException;
import io.github.localsoul.runtime.system.Strings;
import io.github.localsoul.runtime.system.SystemException;
import io.github.localsoul.runtime.system.collections.generic.Dictionary;
import io.github.localsoul.runtime.system.collections.generic.KeyNotFoundException;
import io.github.localsoul.runtime.system.collections.generic.KeyValuePair;

/** Generiert aus dem IR-Typ {@code Ledger}. Nicht bearbeiten, Handarbeit in der Unterklasse. */
public abstract class LedgerBase {
  Dictionary<String, Integer> entries = new Dictionary<>();

  public String check(int amount) {
    try {
      if (amount < 0) {
        throw new InvalidOperationException("negativ");
      }
      return "ok";
    } catch (SystemException | NullPointerException | ArrayIndexOutOfBoundsException ex$) {
      SystemException ex = SystemException.fromJava(ex$);
      return ex.message();
    }
  }

  public String format(float value) {
    return Strings.concat("v=", value);
  }

  public String guard(Ledger other) {
    String log = "try;";
    try {
      other.entries.clear();
      log = Strings.concat(log, "ok;");
    } catch (NullReferenceException | NullPointerException ex$) {
      NullReferenceException ex = NullReferenceException.fromJava(ex$);
      log = Strings.concat(Strings.concat(Strings.concat(log, "nre:"), ex.message()), ";");
    } finally {
      log = Strings.concat(log, "finally");
    }
    return log;
  }

  public int lookup(String key) {
    try {
      return this.entries.get(key);
    } catch (KeyNotFoundException __ex) {
      return -1;
    }
  }

  public String order() {
    this.entries.clear();
    this.entries.add("a", 1);
    this.entries.add("b", 2);
    this.entries.add("c", 3);
    this.entries.remove("b");
    this.entries.add("d", 4);
    String text = "";
    for (KeyValuePair<String, Integer> pair : this.entries) {
      text =
          Strings.concat(
              Strings.concat(Strings.concat(Strings.concat(text, pair.key()), "="), pair.value()),
              ";");
    }
    return text;
  }
}
