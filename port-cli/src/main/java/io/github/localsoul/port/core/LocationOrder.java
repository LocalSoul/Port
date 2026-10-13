package io.github.localsoul.port.core;

import java.util.Comparator;

/**
 * Ordnet Orte {@code datei:zeile:spalte} nach Datei, dann numerisch nach Zeile und Spalte (wie der
 * IR-Validator); ohne Ort zuerst.
 */
final class LocationOrder implements Comparator<String> {

  static final LocationOrder INSTANCE = new LocationOrder();

  private LocationOrder() {}

  @Override
  public int compare(String a, String b) {
    String[] left = a.split(":", -1);
    String[] right = b.split(":", -1);
    int byFile = left[0].compareTo(right[0]);
    if (byFile != 0) {
      return byFile;
    }
    for (int i = 1; i < java.lang.Math.max(left.length, right.length); i++) {
      long l = i < left.length ? number(left[i]) : -1;
      long r = i < right.length ? number(right[i]) : -1;
      if (l != r) {
        return Long.compare(l, r);
      }
    }
    return a.compareTo(b);
  }

  private static long number(String text) {
    try {
      return Long.parseLong(text);
    } catch (NumberFormatException e) {
      return Long.MAX_VALUE;
    }
  }
}
