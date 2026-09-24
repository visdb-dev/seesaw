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

import java.awt.EventQueue;

import javax.swing.JComponent;

import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;

import dev.visdb.seesaw.utils.SsComponent;

/**
 * 
 */
public class DefaultSsComponentValidationItem extends ValidationListener<SsComponent> {
  private final Validator<SsComponent> validator;

  public DefaultSsComponentValidationItem(SsComponent component, ValidationUI ui,
                                          Validator<SsComponent> validator) {
    super(SsComponent.class, ui, component);
    this.validator = validator;
  }
  
  /**
   * @return The JTextComponent that this ValidationItem is hooked to.
   */
  public SsComponent getComponent() {
    return getTarget();
  }
  
  /** Run the validator.
   * @param problems */
  @Override
  protected void performValidation(Problems problems) {
    if (!EventQueue.isDispatchThread()) {
      EventQueue.invokeLater(() -> performValidation(problems));
    }
    SsComponent component = getTarget();
    if (!((JComponent)component).isEnabled()) {
      return;
    }
    validator.validate(problems, SwingValidationGroup.nameForComponent((JComponent)component),
                                     component);
  }
}
// vi: sw=2 ts=8