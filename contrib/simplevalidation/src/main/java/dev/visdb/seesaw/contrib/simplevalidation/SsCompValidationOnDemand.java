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
import javax.swing.SwingUtilities;

import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;

import dev.visdb.seesaw.utils.SsComponent;

/**
 * On demand SsComponent validation, see
 * {@link SVUtils#setDecorator(dev.visdb.seesaw.utils.SsComponent,
 * java.lang.String, org.netbeans.validation.api.Validator)
 * SVUtils.setDecorator(ssComp, name, validator)}.
 * SeeSaw decorator invokes this as needed.
 * @param <T>
 */
public class SsCompValidationOnDemand<T extends SsComponent> extends ValidationListener<T> {
  private final Validator<T> validator;
  
  /**
   * Create ValidationItem, used only on demand, for the specified component.
   * 
   * @param ssComp
   * @param ui
   * @param validator 
   */
  @SuppressWarnings("unchecked") // unchecked cast (Class<T>)
  public SsCompValidationOnDemand(T ssComp, ValidationUI ui,
                                       Validator<T> validator) {
    super((Class<T>)ssComp.getClass(), ui, ssComp);
    this.validator = validator;
  }
  
  /**
   * Run validation on the SsComponent.
   * @param ps
   */
  @Override
  protected void performValidation(Problems ps) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> performValidation(ps));
    }
    JComponent jc = (JComponent)getTarget();
    if (!jc.isEnabled()) {
      return;
    }
    validator.validate(ps, SwingValidationGroup.nameForComponent(jc), getTarget());
  }
}
// vi: sw=2 ts=8