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

import org.netbeans.validation.api.Problem;
import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Severity;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.netbeans.validation.api.ui.swing.ValidationPanel;

import dev.visdb.seesaw.decorators.BaseDecorator;
import dev.visdb.seesaw.decorators.ComponentState;
import dev.visdb.seesaw.decorators.SsCompFocusListener;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsComponent.Validation;
import dev.visdb.seesaw.utils.SsComponent.ValidationResult;


/**
 * This decorator can be attached to any SsComponent.
 * It is installed during component initialization when the default
 * decorator style is SVUtils.SV_DECORATOR_STYLE,
 * see {@link SVDecoratorFactory}.
 * The associated validationItem is created by decorate().
 * <p>
 * Although this decorator is attached during component initialization; some
 * things that affect decoration may be set aftwards. So there is messy code
 * to detect these changes and rebuild the ValidationItem.
 */
// and it is a combined ValidationItem/Validator,
// see DefaultValidationItem below.
public class DefaultSVDecorator extends BaseDecorator {
  /** create Decorator */
  public DefaultSVDecorator() {
  }
  
  private ValidationListener<SsComponent> valItem;
  private JComponent decoratorTarget;

  private SVBorder border;
  private SsCompFocusListener focusListener;

  /**
   * {@inheritDoc }
   */
  @Override
  public void install(SsComponent ssComp) {
    super.install(ssComp);
    border = SVBorder.get(getSsComponent());
    focusListener = new SsCompFocusListener(ssComp, (c) -> {
      // System.err.printf("*** DefaultSVDecorator (%s) - notifyFocusChange: %s, focused: %s\n",
      //                     objectID(component), objectID(c), c.isFocusOwner());
      if (c.isFocusOwner())
        border.draw();
      else
        border.clear();
    });
    focusListener.addFocusListener();
  }

  /**
   * Remove the ValidationItem from ValidationPanel.
   */
  @Override
  public void uninstall() {
    ValidationPanel valiPanel = SVUtils.findDecoratorPanel(getSsComponent());
    if (valiPanel != null && valItem != null)
      valiPanel.getValidationGroup().remove(valItem);
    focusListener.removeFocusListener();
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
    // The valItem, produced by the DefaultValidationItem, does text decoration.
    return problem == null || !problem.isFatal();
  }

  /**
   * Create the ValidationItem if needed.
   * decoratorTarget may be set an anytime;
   * check if it has changed and recreate if needed.
   */
  // TODO: Might want DecorationFactory that takes a Supplier<JComponent>
  //       then can avoid this mess.
  private boolean isValItemOK() {
    JComponent currentDecoratorTarget = decoComp();
    boolean newDecoratorTarget = currentDecoratorTarget != decoratorTarget;
    if (foundValidationPanel && !newDecoratorTarget)
      return true;
    ValidationPanel valiPanel = SVUtils.findDecoratorPanel(getSsComponent());
    if (valiPanel == null)
      return false;

    if (newDecoratorTarget || valItem == null)
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
    valItem = createValItem(ssComp);
    valiPanel.getValidationGroup().addItem(valItem, false);
  }
  
  /**
   * SimpleValidatorDecorator style
   * @return
   */
  @Override
  public DecoratorStyle getDecoratorStyle() {
    return SVUtils.SIMPLE_VALIDATION;
  }

  //
  // The following used to be ValidaitonItemFactory.
  //

  private ValidationListener<SsComponent> createValItem(SsComponent ssComp) {
    SwingValidationGroup.setComponentName((JComponent)ssComp, ssComp.getColumnName());
    ValidationUI ui = SVUtils.decorationFor(ssComp);
    return new DefaultValidationItem(ssComp, ui);
  }

  private static class DefaultValidationItem extends ValidationListener<SsComponent> {
    
    public DefaultValidationItem(SsComponent ssComp, ValidationUI ui) {
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
      ComponentState state = ComponentState.getComponentState(ssComp, vr);
      if (state.isModified())
        problems.append("modified", Severity.INFO);
      
      Optional<Validation> fail = vr.firstFail();
      if (fail.isPresent())
        problems.append(ssComp.validationMsg(fail.get()));

      // This is normally at the end of the SeeSaw decorator.
      ssComp.decorateText(vr);
    }
  }
}
// vi: sw=2 ts=8