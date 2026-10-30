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

package dev.visdb.seesaw.contrib.simplevalidation;


import java.awt.Component;
import java.awt.Container;

import org.netbeans.validation.api.ui.swing.ValidationPanel;
import org.openide.util.lookup.ServiceProvider;

import dev.visdb.seesaw.decorators.DecoratorFactory;
import dev.visdb.seesaw.decorators.DecoratorFactoryBase;

/**
 * Handles SimpleValidation.
 */
@ServiceProvider(path = DecoratorFactory.DECORATOR_PATH, service = DecoratorFactory.class)
public class SVDecoratorFactory extends DecoratorFactoryBase {
  /** Create a DefaultSVDecorator */
  public SVDecoratorFactory() {
    super(() -> new DefaultSVDecorator(), SVUtils.SIMPLE_VALIDATION);
  }
  
  /**
   * {@inheritDoc }
   * <p>
   * Wrap the uiPanel in a {@link ValidationPanel}.
   */
  @Override
  public ValidationPanel createDecoratorPanel(Component uiPanel) {
    return SVUtils.createDecoratorPanel((Container)uiPanel);
  }
}
// vi: sw=2 ts=8
