/* *****************************************************************************
 * Portions created by Ernie Rael are
 * Copyright (C) 2026 Ernie Rael.  All Rights Reserved.
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at https://mozilla.org/MPL/2.0/.
 *
 * Contributor(s): Ernie Rael <errael@raelity.com>
 * ****************************************************************************/

package dev.visdb.seesaw.decorators;

import java.awt.Component;

import javax.swing.JPanel;

/**
 * Typically extend {@link DecoratorSupplierBase}.
 * {@snippet lang="java" class=SomeDecoratorSupplier region=decorator_supplier_1}
 * <p>
 * If you want to provide your own DecoratorSupplier for a given style
 * then add a position param, less than max int, to the ServerProvider
 * declaration.
 */
public interface DecoratorSupplier {
  /** these named services go here */
  public static String DECORATOR_PATH = "SS/Decorator";
  /**
   * Create and return a decorator of the expected type.
   * @return decorator
   */
  Decorator get();

  /**
   * Register the given panel for use with SeeSaw decorators. The uiPanel
   * is used to create the gui. The returned parentPanel wraps the uiPanel
   * and is typically added to a frame. For example
   * {@snippet lang="java" class=Decorators region=decorator_panel_1}
   * @param uiPanel
   * @return
   */
  JPanel createDecoratorPanel(Component uiPanel);

  /**
   * Decorator style.
   * @return decorator style
   */
  Decorator.DecoratorStyle getDecoratorStyle();
}
// vi: sw=2 ts=8
