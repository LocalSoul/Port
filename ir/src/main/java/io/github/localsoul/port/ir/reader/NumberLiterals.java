package io.github.localsoul.port.ir.reader;

import io.github.localsoul.port.ir.IrSyntaxException;
import io.github.localsoul.port.ir.sexpr.NumberSuffix;
import io.github.localsoul.port.ir.sexpr.SBareInteger;
import io.github.localsoul.port.ir.sexpr.SNumber;
import java.math.BigInteger;

/**
 * Prüft Wertebereiche von Zahlen.
 *
 * <ul>
 *   <li>Dezimale Ganzzahlen müssen in den Typ passen, vorzeichenlose Typen erlauben kein {@code -}.
 *   <li>Hexzahlen sind Bitmuster: Sie dürfen höchstens so viele Bits haben wie der Typ ({@code
 *       0xFFFFFFFFi32} ist erlaubt und bedeutet -1).
 *   <li>Gleitkommazahlen dürfen nicht auf Unendlich überlaufen.
 * </ul>
 */
final class NumberLiterals {

  private static final BigInteger MAX_CHAR = BigInteger.valueOf(0xFFFF);

  private NumberLiterals() {}

  /** Prüft den Wertebereich einer Zahl mit Suffix. */
  static void checkRange(SNumber number) throws IrSyntaxException {
    NumberSuffix suffix = number.suffix();
    String text = number.text();
    String digits = text.substring(0, text.length() - suffix.spelling().length());
    if (suffix.isFloatingPoint()) {
      checkFloat(number, digits);
      return;
    }
    int bits = Integer.parseInt(suffix.spelling().substring(1));
    boolean unsigned = suffix.spelling().charAt(0) == 'u';
    if (digits.startsWith("0x")) {
      BigInteger value = new BigInteger(digits.substring(2), 16);
      if (value.bitLength() > bits) {
        throw outOfRange(number);
      }
      return;
    }
    BigInteger value = new BigInteger(digits);
    BigInteger min;
    BigInteger max;
    if (unsigned) {
      min = BigInteger.ZERO;
      max = BigInteger.ONE.shiftLeft(bits).subtract(BigInteger.ONE);
    } else {
      min = BigInteger.ONE.shiftLeft(bits - 1).negate();
      max = BigInteger.ONE.shiftLeft(bits - 1).subtract(BigInteger.ONE);
    }
    if (digits.startsWith("-") && unsigned) {
      throw new IrSyntaxException(
          number.span(), "negativer Wert für vorzeichenlosen Typ in '" + text + "'");
    }
    if (value.compareTo(min) < 0 || value.compareTo(max) > 0) {
      throw outOfRange(number);
    }
  }

  private static void checkFloat(SNumber number, String digits) throws IrSyntaxException {
    boolean infinite;
    if (number.suffix() == NumberSuffix.F32) {
      infinite = Float.isInfinite(Float.parseFloat(digits));
    } else {
      infinite = Double.isInfinite(Double.parseDouble(digits));
    }
    if (infinite) {
      throw outOfRange(number);
    }
  }

  /** Liest eine UTF-16-Code-Unit für {@code (char nat)}. */
  static char charValue(SBareInteger value) throws IrSyntaxException {
    BigInteger number = new BigInteger(value.text());
    if (number.compareTo(MAX_CHAR) > 0) {
      throw new IrSyntaxException(
          value.span(), "char-Wert außerhalb von 0 bis 65535: " + value.text());
    }
    return (char) number.intValue();
  }

  /** Liest ein {@code nat} als {@code int}. */
  static int intValue(SBareInteger value) throws IrSyntaxException {
    BigInteger number = new BigInteger(value.text());
    if (number.bitLength() > 31) {
      throw new IrSyntaxException(value.span(), "Zahl zu groß: " + value.text());
    }
    return number.intValue();
  }

  private static IrSyntaxException outOfRange(SNumber number) {
    return new IrSyntaxException(
        number.span(),
        "Wert außerhalb des Bereichs von "
            + number.suffix().spelling()
            + ": '"
            + number.text()
            + "'");
  }
}
