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

import java.util.Objects;

/**
 * An item that acts kind of like a standalone SSListItem.
 *
 * @param <K>
 * @param <D>
 * @param <D2>
 */
public class Item2<K, D, D2> {
  private final K key;
  private final D displayValue;
  private final D2 d2;

  /** Use this for D2 arg of constructor, if there is no D2. */
  public static final Object NO_D2 = new Object();

  /**
   * Create immutable item.
   * @param key
   * @param displayValue
   * @param d2
   */
  public Item2(K key, D displayValue, D2 d2) {
    this.key = key;
    this.displayValue = displayValue;
    this.d2 = d2;
  }

  /**
   * Create immutable item which does not have a d2.
   * @param key
   * @param displayValue
   */
  @SuppressWarnings("unchecked")
  Item2(K key, D displayValue) {
    this(key, displayValue, (D2) NO_D2);
  }

  //
  // It seems this never worked. Note the "this(null, null, null);".
  // ComboBox2.getChosenItem(fnConstructItem) is pretty good.
  //
  // /**
  //  * Create immutable item based on SSListItem.
  //  * @param listItem
  //  * @param hasD2
  //  */
  // @SuppressWarnings("unchecked")
  // Item2(SSListItem listItem, boolean hasD2) {
  //   // li = (ListItem0)listItem;
  //   // key = (K)li.getElem(OptionMappingSwingModel.KEY_IDX);
  //   // displayValue = (D)li.getElem(OptionMappingSwingModel.DISP_IDX);
  //   // handle D2
  //   this(null, null, null);
  // }

  /**
   * key getter.
   * @return
   */
  public K getKey() {
    return key;
  }

  /**
   * displayValue getter.
   * @return
   */
  public D getDisplayValue() {
    return displayValue;
  }

  /**
   * d2 getter.
   * Exception if does not have a d2
   * @return
   */
  public D2 getD2() {
    if (d2 == NO_D2)
      throw new IllegalStateException("Item does not have d2");
    return d2;
  }

  /**
   * Check if this has a d2
   * @return true if there is a d2
   */
  public boolean hasD2() {
    return d2 != NO_D2;
  }

  /**
   * hashCode.
   * @return
   */
  @Override
  public int hashCode() {
    int hash = 7;
    hash = 67 * hash + Objects.hashCode(this.key);
    hash = 67 * hash + Objects.hashCode(this.displayValue);
    hash = 67 * hash + Objects.hashCode(this.d2);
    return hash;
  }

  /**
   * equals.
   * @param obj
   * @return
   */
  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
                final Item2<?, ?, ?> other = (Item2<?, ?, ?>) obj;
                if (!Objects.equals(this.key, other.key))
                  return false;
                if (!Objects.equals(this.displayValue, other.displayValue))
                  return false;
                return Objects.equals(this.d2, other.d2);
  }
}

/*
public record Item2<K,D,D2>(K getKey, D getDisplayValue, D2 getD2)
{
        private static final Object NO_D2 = new Object();

        @SuppressWarnings("unchecked")
        public Item2(K getKey, D getDisplayValue)
        {
                this(getKey, getDisplayValue, (D2)NO_D2);
        }

        @SuppressWarnings("unchecked")
        Item2(SSListItem listItem, boolean hasD2)
        {
                // li = (ListItem0)listItem;
                // key = (K)li.getElem(OptionMappingSwingModel.KEY_IDX);
                // displayValue = (D)li.getElem(OptionMappingSwingModel.DISP_IDX);
                // handle D2
                this(null, null, null);
        }

        @Override
        public D2 getD2()
        {
                if (getD2 == NO_D2)
                        throw new IllegalStateException("Item2 does not have d2");
                return getD2;
        }

        public boolean hasD2()
        {
                return getD2 != NO_D2;
        }
}
 */
// vi: sw=2 ts=8
