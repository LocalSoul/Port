package io.github.localsoul.port.core;

/** Kennungen der Diagnosen, die die CLI selbst erzeugt (Liste in {@code docs/cli-contract.md}). */
public final class DiagnosticIds {

  /** Ungültige Argumente. */
  public static final String ARGS = "PORT-ARGS";

  /** Ungültiges oder fehlendes Manifest. */
  public static final String MANIFEST = "PORT-MANIFEST";

  /** Projekt existiert bereits. */
  public static final String EXISTS = "PORT-EXISTS";

  /** Kein Git-Repo angelegt (M4). */
  public static final String GIT = "PORT-GIT";

  /** Eingabedatei fehlt. */
  public static final String INPUT = "PORT-INPUT";

  /** Hash der Eingabe stimmt nicht. */
  public static final String HASH = "PORT-HASH";

  /** Frontend fehlt oder ist fehlgeschlagen. */
  public static final String FRONTEND = "PORT-FRONTEND";

  /** Nicht aufgelöste Typen in der IR (fehlende Referenz-Assemblies). */
  public static final String UNRESOLVED = "PORT-UNRESOLVED";

  /** IR-Datei ungültig. */
  public static final String IR = "PORT-IR";

  /** Generator konnte ein Member nicht erzeugen. */
  public static final String TODO = "PORT-TODO";

  /** Offene Handarbeit ({@code raw}/{@code :manual} ohne {@code manual/}-Datei). */
  public static final String MANUAL = "PORT-MANUAL";

  /** Kompilierfehler. */
  public static final String COMPILE = "PORT-COMPILE";

  /** Plattform (Adapter, runtime-system, Frontend) nicht gefunden. */
  public static final String PLATFORM = "PORT-PLATFORM";

  /** Funktion braucht einen späteren Meilenstein. */
  public static final String LATER = "PORT-LATER";

  /** Ein-/Ausgabefehler. */
  public static final String IO = "PORT-IO";

  private DiagnosticIds() {}
}
