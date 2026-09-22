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
package dev.visdb.seesaw.decorators;

import java.util.function.Supplier;

/**
 * Like a factory for Decorator.
 */
public class DecoratorSupplier {
  private final Supplier<Decorator> supplier;
  private final Decorator.DecoratorStyle style;

  /**
   * create a decorator supplier
   * @param supplier
   */
  public DecoratorSupplier(Supplier<Decorator> supplier) {
    this(supplier, supplier.get().getDecoratorStyle());
  }

  /**
   * create a decorator supplier
   * @param supplier
   * @param style
   */
  public DecoratorSupplier(Supplier<Decorator> supplier, Decorator.DecoratorStyle style) {
    this.supplier = supplier;
    this.style = style;
  }

  /**
   * Create and return a decorator.
   * @return decorator
   */
  public Decorator get() {
    return supplier.get();
  }

  /**
   * Decorator style.
   * @return decorator style
   */
  public Decorator.DecoratorStyle getDecoratorStyle() {
    return style;
  }
}
// vi: sw=2 ts=8
