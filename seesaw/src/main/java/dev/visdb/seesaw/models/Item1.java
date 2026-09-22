/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw.models;

/**
 * An item that acts kind of like a standalone SSListItem.
 *
 * @param <K>
 * @param <D>
 */
public class Item1<K, D> extends Item2<K, D, Object> {
  /**
   * Create immutable item.
   * @param key
   * @param displayValue
   */
  public Item1(K key, D displayValue) {
    super(key, displayValue);
  }

  // /**
  //  * Create immutable item based on SSListItem.
  //  * @param listItem
  //  */
  // public Item1(SSListItem listItem) {
  //   super(listItem, false);
  // }
}
// vi: sw=2 ts=8
