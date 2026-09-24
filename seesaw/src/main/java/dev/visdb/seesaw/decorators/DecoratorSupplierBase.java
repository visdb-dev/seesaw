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

import java.awt.BorderLayout;
import java.awt.Component;
import java.util.function.Supplier;

import javax.swing.JPanel;

/**
 * Make a DecoratorSupplier out of this.
 * 
 */
public abstract class DecoratorSupplierBase implements DecoratorSupplier {
  private final Supplier<Decorator> supplier;
  private final Decorator.DecoratorStyle style;

  /**
   * create a decorator supplier
   * @param supplier
   * @param style
   */
  public DecoratorSupplierBase(Supplier<Decorator> supplier, Decorator.DecoratorStyle style) {
    this.supplier = supplier;
    this.style = style;
  }

  /**
   * Create and return a decorator.
   * @return decorator
   */
  @Override
  public Decorator get() {
    return supplier.get();
  }

  /**
   * Register the given panel for use with SeeSaw decorators. The uiPanel
   * is used to create the gui. The returned parentPanel wraps the uiPanel
   * and is typically added to a frame. For example
   * {@snippet lang="java" class=Decorators region=decorator_panel_1}
   * <p>
   * This implementation is used when a specialized decorator panel
   * is not required.
   * @param uiPanel
   * @return 
   */
  @Override
  public JPanel createDecoratorPanel(Component uiPanel) {
    // Could just do: uiPanel.putClientProperty return uiPanel;
    JPanel parentPanel = new JPanel(new BorderLayout());
    parentPanel.add(uiPanel, BorderLayout.CENTER);
    parentPanel.putClientProperty(Decorator.SEE_SAW_PANEL_KEY, getDecoratorStyle());
    return parentPanel;
    //return uiPanel;
  }

  /**
   * Decorator style.
   * @return decorator style
   */
  @Override
  public Decorator.DecoratorStyle getDecoratorStyle() {
    return style;
  }

  /** style */
  @Override
  public String toString() {
    return getDecoratorStyle().toString();
  }
}
// vi: sw=2 ts=8
