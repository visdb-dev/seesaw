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
import org.netbeans.validation.api.ui.ValidationListener;
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
  
  private ValidationListener<SsComponent> valItem;
  private JComponent decoratorTarget;

  /** create ValidationItem
   * @param ssComp */
  @Override
  public void install(SsComponent ssComp) {
    super.install(ssComp);
  }

  /**
   * Remove the ValidationItem from ValidationPanel.
   */
  @Override
  public void uninstall() {
    ValidationPanel valiPanel = SVUtils.findDecoratorPanel((JComponent)getSsComponent());
    if (valiPanel != null && valItem != null)
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
    if (!isValItemOK())
      return true;
    Problem problem = valItem.performValidation();
    return problem == null || !problem.isFatal();
  }

  // TODO: Might want DecorationFactory that takes a Supplier<JComponent>
  //       then can avoid this mess.
  private boolean isValItemOK() {
    // decoratorTarget may be set an anytime; check if it has changed
    JComponent currentDecoratorTarget = getSsComponent().getDecorateTarget();
    boolean newTarget = currentDecoratorTarget != decoratorTarget;
    if (foundValidationPanel && !newTarget)
      return true;
    ValidationPanel valiPanel = SVUtils.findDecoratorPanel((JComponent)getSsComponent());
    if (valiPanel == null)
      return false;

    if (newTarget || valItem == null)
      newValItem(valiPanel, currentDecoratorTarget);

    foundValidationPanel = true;
    return true;
  }

  // valiPanel must exist
  private void newValItem(ValidationPanel valiPanel, JComponent decoratorTarget) {
    SsComponent ssComp = getSsComponent();
    if (valItem != null) {
      valiPanel.getValidationGroup().remove(valItem);
    }
    this.decoratorTarget = decoratorTarget;
    valItem = ValidationItemFactory.get(ssComp, decoratorTarget);
    valiPanel.getValidationGroup().addItem(valItem, false);
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