/**
 * Java-Nachbau des .NET-Basisbibliothek-Ausschnitts ({@code System.*}), den übersetzter Spielcode
 * nutzt ({@code docs/modules/runtime-system.md}, ADR-019). Pakete spiegeln die .NET-Namespaces.
 *
 * <p>Semantik nach .NET Framework / Unity-Mono (Nutzerentscheidung M3a): Zahlformat „G7“/„G15“,
 * Dictionary-Reihenfolge wie .NET, Exceptions mit .NET-Namen. Jeder Unterschied zu Java ist
 * getestet.
 */
package io.github.localsoul.runtime.system;
