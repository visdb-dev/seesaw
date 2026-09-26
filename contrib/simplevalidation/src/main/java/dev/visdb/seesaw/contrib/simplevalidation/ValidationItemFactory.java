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
import java.util.Optional;

import javax.swing.JComponent;

import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingComponentDecorationFactory;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;

import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsComponent.Validation;
import dev.visdb.seesaw.utils.SsComponent.ValidationResult;

/**
 * This class provides an on demand validation item for an SsComponent
 * to satisfy global SV_DECORATOR_STYLE when there is no explicit ValidationItem.
 */
class ValidationItemFactory {
  private ValidationItemFactory() { }
  
  /**
   * Not really a listener, more like a ValidationItem,
   * but we want the getTarget() method.
   * @param comp
   * @return
   */
  static ValidationListener<SsComponent> get(SsComponent ssComp, JComponent decorationTarget) {
    SwingValidationGroup.setComponentName((JComponent)ssComp, ssComp.getColumnName());
    ValidationUI ui = SVUtils.decorationFor(ssComp);
    ValidationListener<SsComponent> valItem = new SsComponentValidationItem(ssComp, ui);

    return valItem;
  }

    // FYI - to use ValidationListenerFactory
    // valItem = ValidationListenerFactory.createValidationListener(ssComp,
    //     ValidationStrategy.ON_FOCUS_LOSS,
    //     ui,
    //     new DefaultSsComponentValidator((JComponent)ssComp));
    // removeListeners(ssComp, valItem);
  
  /**
   * On demand (no listeners).
   */
  static class SsComponentValidationItem extends ValidationListener<SsComponent> {
    
    public SsComponentValidationItem(SsComponent ssComp, ValidationUI ui) {
      super(SsComponent.class, ui, ssComp);
    }
    
    /** Run the validator.
     * @param problems */
    @Override
    protected void performValidation(Problems problems) {
      if (!EventQueue.isDispatchThread()) {
        EventQueue.invokeLater(() -> performValidation(problems));
      }
      SsComponent ssComp = getTarget();
      if (!((JComponent)ssComp).isEnabled()) {
        return;
      }
      // This would normally be validator.validate(...)
      validate(problems, SwingValidationGroup.nameForComponent((JComponent)ssComp));
    }
    
    private void validate(Problems problems, @SuppressWarnings("unused") String compName) {
      SsComponent ssComp = getTarget();
      ValidationResult vr = ssComp.allValidate();
      ComponentState borderState = ComponentState.getComponentState(ssComp, vr);
      if (borderState.isModified())
        problems.append("modified", Severity.INFO);
      
      Optional<Validation> fail = vr.firstFail();
      if (fail.isPresent())
        problems.append(ssComp.validationMsg(fail.get()));
    }
  }

    // TODO: use try catch IllegalArgumentException "No registered..."
    // and try the "Validating custom components" in original docs.
    // valItem = switch (ssComp) {
    //   case SsCheckBox cb -> new SsComponentValidationItem(cb, ui);
    //   case SsImage im -> new SsComponentValidationItem(im, ui);
    //   case SsLabel la -> new SsComponentValidationItem(la, ui);
    //   case SsSlider sl -> new SsComponentValidationItem(sl, ui);
    //   default -> new SsComponentValidationItem(ssComp, ui);
    //   // default -> ValidationListenerFactory.createValidationListener(ssComp,
    //   //   ValidationStrategy.ON_FOCUS_LOSS,
    //   //   ui,
    //   //   new DefaultSsComponentValidator((JComponent)ssComp));
    // };
    // valItem = ValidationListenerFactory.createValidationListener(ssComp,
    //     ValidationStrategy.ON_FOCUS_LOSS,
    //     ui,
    //     new DefaultSsComponentValidator((JComponent)ssComp));

    // removeListeners(ssComp, valItem);

    // Use strategy ON_FOCUS_LOSS 
    // var valItem = new DefaultSsComponentValidationItem(ssComp,
    //     SwingComponentDecorationFactory.getDefault().decorationFor((JComponent)ssComp),
    //     new DefaultSsComponentValidator(ssComp));

  // private static void removeListeners(SsComponent ssComp, ValidationListener<?> valItem) {
  //   if (ssComp instanceof JComponent jc) {
  //     for(FocusListener focusListener : jc.getFocusListeners()) {
  //       if (focusListener == valItem)
  //         jc.removeFocusListener(focusListener);
  //     }
  //     removeListener:
  //     for(PropertyChangeListener propertyChangeListener
  //         : jc.getPropertyChangeListeners("enabled")) {
  //       Class<?> clazz = propertyChangeListener.getClass();
  //       do {
  //         if (clazz.equals(valItem.getClass())) {
  //           jc.removePropertyChangeListener("enabled", propertyChangeListener);
  //           break removeListener;
  //         }
  //         clazz = propertyChangeListener.getClass().getEnclosingClass();
  //       } while(clazz != null);
  //     }
  //   }
  //   // TODO: consider model of button[], changelistener, itemlistener
  // }
}
// vi: sw=2 ts=8
