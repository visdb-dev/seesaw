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

import org.netbeans.validation.api.ui.swing.ValidationPanel;
import org.openide.util.lookup.ServiceProvider;

import dev.visdb.seesaw.decorators.DecoratorSupplier;
import dev.visdb.seesaw.decorators.DecoratorSupplierBase;

/**
 * Handles SimpleValidation.
 */
@ServiceProvider(path = DecoratorSupplier.DECORATOR_PATH, service = DecoratorSupplier.class)
public class SimpleValidationDecoratorSupplier extends DecoratorSupplierBase {
  
  /** Create SV DecoratorSupplierBase */
  public SimpleValidationDecoratorSupplier() {
    super(() -> new DefaultValidationDecorator(), SVUtils.SV_DECORATOR_STYLE);
  }
  
  /**
   * {@inheritDoc }
   * <p>
   * Wrap the uiPanel in a {@link ValidationPanel}.
   */
  @Override
  public ValidationPanel createDecoratorPanel(Component uiPanel) {
    return SVUtils.createDecoratorPanel(uiPanel);
  }
}
// vi: sw=2 ts=8
