/* *****************************************************************************
 * Copyright (C) 2025, Ernie R Rael. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
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
