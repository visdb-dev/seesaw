/*
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 */

package dev.visdb.seesaw.decorators;

import org.openide.util.lookup.ServiceProvider;

import dev.visdb.seesaw.decorators.Decorator.DecoratorStyle;

/**
 * Create BackgroundDecorator related objects.
 */
@ServiceProvider(path = DecoratorFactory.DECORATOR_PATH, service = DecoratorFactory.class)
public class BackgroundDecoratorFactory extends DecoratorFactoryBase {
  /** Create the factory. */
  public BackgroundDecoratorFactory() {
    super(() -> new BackgroundDecorator(), DecoratorStyle.BACKGROUND);
  }
}
// vi: sw=2 ts=8