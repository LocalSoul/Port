package io.github.localsoul.port.engine.api.world;

import io.github.localsoul.port.engine.api.math.Mat4;
import java.util.List;

/**
 * Die Welt: Entities und ihre Komponenten (ECS, ADR-016).
 *
 * <p><b>Strukturänderungen</b> ({@link #destroy}, {@link #add}, {@link #remove}, {@link
 * #setParent}) während einer Phase werden gesammelt und am Ende dieser Phase in Aufrufreihenfolge
 * ausgeführt; außerhalb eines Frames (Szenenaufbau) wirken sie sofort. Befehle für Entities, die
 * bis dahin zerstört wurden, entfallen. {@link #create} und {@link #set} wirken immer sofort.
 *
 * <p><b>Komponenten</b> sind Werte (Records). Pro Entity gibt es höchstens eine Komponente je Typ.
 * Jeder Typ muss vorher mit {@link #registerComponent} registriert sein. {@code toString()} der
 * Komponenten muss deterministisch sein, weil {@link #dump()} es nutzt.
 *
 * <p><b>Vom Core verwaltet</b> sind {@code Parent}, {@code Children} (über {@link #setParent}) und
 * {@code WorldTransform} (berechnet zu Beginn von {@code LATE}); direkte Schreibzugriffe darauf
 * werfen {@link ReservedComponentException}.
 *
 * <p>Alle Methoden mit Entity-Parameter werfen {@link StaleEntityException}, wenn die Entity nicht
 * (mehr) lebt.
 */
public interface World {

  /**
   * Erzeugt eine Entity ohne Komponenten. Wirkt sofort, auch während einer Phase.
   *
   * @return die neue Entity
   */
  Entity create();

  /**
   * Zerstört eine Entity samt aller Kinder (rekursiv, Kinder vor dem Elternobjekt).
   *
   * @param entity Entity
   */
  void destroy(Entity entity);

  /**
   * Prüft, ob die Entity lebt. Wirft nie.
   *
   * @param entity Entity
   * @return {@code true}, wenn Index und Generation zu einer lebenden Entity passen
   */
  boolean isAlive(Entity entity);

  /**
   * Liefert alle lebenden Entities, aufsteigend nach Index.
   *
   * @return unveränderliche Liste
   */
  List<Entity> entities();

  /**
   * Registriert einen Komponententyp (Erweiterungspunkt für Adapter). Mehrfaches Registrieren
   * desselben Typs ist erlaubt und ändert nichts.
   *
   * @param <T> Komponententyp
   * @param type Klasse des Typs
   */
  <T> void registerComponent(Class<T> type);

  /**
   * Hängt eine Komponente an (Strukturänderung). Der Typ ist die Klasse von {@code component}.
   *
   * @param <T> Komponententyp
   * @param entity Entity
   * @param component Komponente, nicht {@code null}
   * @throws UnregisteredComponentException wenn der Typ nicht registriert ist
   * @throws DuplicateComponentException wenn die Entity bereits eine Komponente dieses Typs hat
   */
  <T> void add(Entity entity, T component);

  /**
   * Ersetzt den Wert einer vorhandenen Komponente. Wirkt sofort und ist keine Strukturänderung. Ist
   * die Komponente in dieser Phase hinzugefügt worden und noch ausstehend, ersetzt {@code set} den
   * ausstehenden Wert (er wird am Phasenende sichtbar).
   *
   * @param <T> Komponententyp
   * @param entity Entity
   * @param component neuer Wert, nicht {@code null}
   * @throws MissingComponentException wenn die Entity keine Komponente dieses Typs hat oder bekommt
   */
  <T> void set(Entity entity, T component);

  /**
   * Liefert eine Komponente.
   *
   * @param <T> Komponententyp
   * @param entity Entity
   * @param type Typ
   * @return die Komponente
   * @throws MissingComponentException wenn die Entity keine Komponente dieses Typs hat
   * @throws UnregisteredComponentException wenn der Typ nicht registriert ist
   */
  <T> T get(Entity entity, Class<T> type);

  /**
   * Prüft, ob die Entity eine Komponente des Typs hat.
   *
   * @param <T> Komponententyp
   * @param entity Entity
   * @param type Typ
   * @return {@code true}, wenn vorhanden
   * @throws UnregisteredComponentException wenn der Typ nicht registriert ist
   */
  <T> boolean has(Entity entity, Class<T> type);

  /**
   * Entfernt eine Komponente (Strukturänderung).
   *
   * @param <T> Komponententyp
   * @param entity Entity
   * @param type Typ
   * @throws MissingComponentException wenn die Entity keine Komponente dieses Typs hat
   */
  <T> void remove(Entity entity, Class<T> type);

  /**
   * Liefert den Speicher eines Typs zur Iteration.
   *
   * @param <T> Komponententyp
   * @param type Typ
   * @return Lesesicht, aufsteigend nach Entity-Index
   * @throws UnregisteredComponentException wenn der Typ nicht registriert ist
   */
  <T> ComponentStore<T> store(Class<T> type);

  /**
   * Setzt oder löst das Elternobjekt (Strukturänderung). Das Kind wird an das Ende der Kinderliste
   * des neuen Elternobjekts gehängt.
   *
   * @param child Kind
   * @param parent neues Elternobjekt oder {@code null} zum Lösen
   * @throws HierarchyException wenn dadurch ein Zyklus entstünde (auch {@code child == parent})
   */
  void setParent(Entity child, Entity parent);

  /**
   * Liefert das Elternobjekt.
   *
   * @param entity Entity
   * @return Elternobjekt oder {@code null}
   */
  Entity parentOf(Entity entity);

  /**
   * Liefert die Kinder in Reihenfolge des Anhängens.
   *
   * @param entity Entity
   * @return unveränderliche Liste, leer ohne Kinder
   */
  List<Entity> childrenOf(Entity entity);

  /**
   * Berechnet die Welttransformation jetzt aus den lokalen {@code Transform}-Komponenten der
   * Elternkette ({@code Eltern * lokal}). Entities ohne {@code Transform} zählen als Identität.
   *
   * @param entity Entity
   * @return Matrix von lokalen in Weltkoordinaten
   */
  Mat4 worldMatrix(Entity entity);

  /**
   * Liefert eine lesbare Momentaufnahme.
   *
   * @param entity Entity
   * @return Ansicht mit allen Komponenten
   */
  EntityView view(Entity entity);

  /**
   * Liefert den gesamten Zustand als Text. Gleicher Zustand ergibt byte-identischen Text: Entities
   * aufsteigend nach Index, Komponenten nach vollem Klassennamen, eine Zeile je Komponente, Ende
   * mit genau einem {@code \n}.
   *
   * @return deterministischer Dump
   */
  String dump();
}
