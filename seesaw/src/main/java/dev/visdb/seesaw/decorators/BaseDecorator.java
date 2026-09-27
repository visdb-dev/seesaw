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

import dev.visdb.seesaw.utils.SsComponent.ValidationResult;

import static dev.visdb.seesaw.utils.JStuff.sf;

/**
 * Historical. This doesn't do anything.
 * should be called at the end of subclass' decorate().
 */
public abstract class BaseDecorator extends BaseAnyDecorator implements Decorator {

  /**
   * {@inheritDoc }
   */
  @Override
  public String toString() {
    return sf("Decorator{%s}", getDecoratorStyle());
  }
}
// vi: sw=2 ts=8
