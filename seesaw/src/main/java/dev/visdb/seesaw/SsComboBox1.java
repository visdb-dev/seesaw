/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2025-2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/
package dev.visdb.seesaw;

import java.util.Optional;

import dev.visdb.seesaw.models.Item1;

/**
 * A ComboBox that only has a single displayValue.
 * See {@link SsComboBox2} for documentation.
 *
 * @param <K> key type
 * @param <D> displayValue type
 */
@SuppressWarnings("serial")
public class SsComboBox1<K, D> extends SsComboBox2<K, D, Object> {
  /**
   * Builder; see {@link SsComboBox2.Builder}.
   * @param <K>
   * @param <D>
   * @param <T>
   */
  public abstract static class AbstractBuilder<K, D, T extends AbstractBuilder<K, D, T>>
      extends SsComboBox2.AbstractBuilder<K, D, Object, T> {}

  /**
   * Builder.
   * @param <K>
   * @param <D>
   */
  public static class Builder<K, D> extends AbstractBuilder<K, D, Builder<K, D>> {
    /** self type idiom */
    @Override
    protected Builder<K, D> self() {
      return this;
    }

    /** create SsComboBox1 */
    @Override
    public SsComboBox1<K, D> build() {
      return new SsComboBox1<>(this);
    }
  }

  /**
   * @param builder
   */
  protected SsComboBox1(AbstractBuilder<K, D, ?> builder) {
    super(builder);
  }

  /**
   * Creates an object of ComboBox with type params of Object.
   * Default: see {@link SsComboBox2}.
   */
  // TODO: this fails because "K" is not concrete.
  public SsComboBox1() {
    this(new SsComboBox1.Builder<>() {});
  }

  /**
   * Return a copy of the chosenItem with methods getKey(), getDisplayValue().
   */
  @Override
  public Item1<K, D> getChosenItem() {
    Optional<Item1<K, D>> item = getChosenItem((remodel, lItem) -> {
      return new Item1<>(remodel.getKey(lItem), remodel.getDisplayValue(lItem));
    });
    return item.orElse(new Item1<>(null, null));
  }
}
// vi: sw=2 ts=8
