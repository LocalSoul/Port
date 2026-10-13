/**
 * Nachbau der benötigten UnityEngine-API auf der Engine-API ({@code
 * docs/modules/adapter-unity.md}).
 *
 * <p>Namen folgen der Konvention für den Generator: Typen behalten ihren Namen ({@code
 * UnityEngine.Object} heißt {@link io.github.localsoul.adapter.unity.UnityObject}, um nicht mit
 * {@code java.lang.Object} zu kollidieren), Methoden und Properties {@code lowerCamelCase},
 * Operatoren statische Methoden. Die Abbildung steht in {@code mapping.tsv}.
 *
 * <p>Koordinaten (ADR-007): Unity ist links-händig, der Core rechts-händig. Umgerechnet wird nur
 * hier, durch Spiegeln der Z-Achse ({@link io.github.localsoul.adapter.unity.Vector3#toCore()}).
 */
package io.github.localsoul.adapter.unity;
