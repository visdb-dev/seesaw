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

import javax.swing.JComponent;

import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.ui.swing.SwingComponentDecorationFactory;
import org.netbeans.validation.api.ui.swing.ValidationPanel;

import dev.visdb.seesaw.decorators.BaseDecorator;
import dev.visdb.seesaw.utils.SsComponent;

/**
 * This decorator can be attached to any component.
 * The actual validator is created during install
 * and added to the validation group during decorate.
 */
public class DefaultValidationDecorator extends BaseDecorator {
  /** create Decorator */
  public DefaultValidationDecorator() {
  }
  
  private DefaultSsComponentValidationItem valItem;

  /** create ValidationItem */
  @Override
  public void install(SsComponent component) {
    super.install(component);
    valItem = new DefaultSsComponentValidationItem(component,
        SwingComponentDecorationFactory.getDefault().decorationFor((JComponent)component),
        new DefaultSsComponentValidator(component));
  }

  /**
   * Remove the ValidationItem from ValidationPanel.
   */
  @Override
  public void uninstall() {
    ValidationPanel valiPanel = SVUtils.findDecoratorPanel((JComponent)getSsComponent());
    if (valiPanel != null)
      valiPanel.getValidationGroup().remove(valItem);
    super.uninstall();
  }
  
  private boolean foundValidationPanel;

  /**
   * {@inheritDoc }
   * @return 
   */
  @Override
  public boolean decorate() {
    if (getSsComponent() != valItem.getComponent())
      throw new IllegalStateException("decorating the wrong component");
    if (!isValItemAdded())
      return true;
    Problem problem = valItem.performValidation();
    return problem == null || !problem.isFatal();
  }

  private boolean isValItemAdded() {
    if (foundValidationPanel)
      return true;
    ValidationPanel valiPanel = SVUtils.findDecoratorPanel((JComponent)getSsComponent());
    if (valiPanel != null) {
      // TODO: just set disableIU to false
      valiPanel.getValidationGroup().addItem(valItem, false);
      foundValidationPanel = true;
    }
    return foundValidationPanel;
  }
  
  /**
   * SimpleValidatorDecorator style
   * @return
   */
  @Override
  public DecoratorStyle getDecoratorStyle() {
    return SVUtils.SV_DECORATOR_STYLE;
  }
}
// vi: sw=2 ts=8