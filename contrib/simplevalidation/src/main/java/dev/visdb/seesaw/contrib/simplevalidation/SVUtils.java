/* *****************************************************************************
 * Copyright (C) 2024, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors
 *    may be used to endorse or promote products derived from this software
 *    without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * Contributors:
 *   Prasanth R. Pasala
 *   Brian E. Pangburn
 *   Diego Gil
 *   Man "Bee" Vo
 *   Ernie R. Rael
 * ****************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package dev.visdb.seesaw.contrib.simplevalidation;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.util.Arrays;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.netbeans.validation.api.ui.ValidationGroup;
import org.netbeans.validation.api.ui.ValidationItem;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingComponentDecorationFactory;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.netbeans.validation.api.ui.swing.ValidationPanel;

import dev.visdb.seesaw.decorators.BorderDecorator.BorderDecoratorPaint;
import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsUtils.DecoratorPanelAdjustSOUTH;

/**
 * Static methods for working with Simple Validation framework.
 */
public class SVUtils {
  private SVUtils() {}

  /** 
   * To make SimpleValidation the default, during startup style do
   * {@snippet lang="java" class=SimpleValidation region=sv_global}
   */
  public static final Decorator.DecoratorStyle SIMPLE_VALIDATION
      = new Decorator.DecoratorStyle("SV_DECORATOR_STYLE");
  /**
   * The SimpleValidation focus border can be disabled.
   * {@snippet lang="java" class=SimpleValidation region=sv_border_enable}
   * The color of the border is taken from {@code Globals}'
   * {@link BorderDecoratorPaint}'s {@code FOCUSED_CLEAN} color.
   */
  public record SimpleValidationBorderEnable(boolean flag){}
  
  /**
   * Merges {@code validators}, if any, with a {@link SsCompStringValidator}
   * to create a {@link TextValidationOnDemand} ValidationItem;
   * creates a {@link SVDecorator} using the new ValidationItem;
   * and sets the decorator on the SsComponent.
   * The SsComponent must be a {@code JTextComponent} subclass.
   * For more information see
   * {@link Validator }, {@link StringValidators}
   * and {@link ValidatorUtils#merge(Validator...) }.
   * The param {@code name} is used for
   * {@link SwingValidationGroup#setComponentName(JComponent, String)}
   * Note that JTextComponent.getName() is used
   * if {@code name} is null or not otherwise set.
   * <p>
   * The caller typically adds the returned ValidationItem to a
   * {@link ValidationGroup}. This can be done manually,
   * but {@link #createDecoratorPanel(Container, ValidationItem...) 
   * createDecorationPanel(uiPanel, ValidationItem...)} is convenient.
   * <p>
   * <b> If {@link SsComponent#isComposite() ssComp.isComposite()}
   * is true</b>, then
   * {@link SsComponent#setDecorateTarget(JComponent)}
   * should be called before setTextDecorator is called.
   * This is typically not an issue.
   * @param ssComp must be a JTextComponent
   * @param name available to customize problem messages
   * @param validators
   * @return ValidationItem to assign to the {@link ValidationGroup}.
   */
  // TODO: Might want DecorationFactory that takes a Supplier<JComponent>
  //       then can avoid the mess in DefaultValidationDecorator.
  @SafeVarargs
  public static ValidationItem setTextDecorator(SsComponent ssComp, String name,
                                                Validator<String>... validators) {
    SwingValidationGroup.setComponentName((JTextComponent)ssComp, name); // type check
    Validator<String>[] newValidatorArray = Arrays.copyOf(validators, validators.length + 1);
    // put the new validator at the end of the array
    newValidatorArray[newValidatorArray.length - 1] = new SsCompStringValidator(ssComp);

    ValidationListener<?> textVali = TextValidationOnDemand.create(ssComp, newValidatorArray);
    // decorateText() is in SsCompStringValidator, so SVDecor's doTextValidation is false
    SVDecorator deco = new SVDecorator(textVali);
    ssComp.setDecorator(deco);
    return textVali;
  }

  /**
   * This method is simple and works with any SsComponent; for an SsComponent
   * that is a subclass of JTextCompoennt use
   * {@link #setTextDecorator(SsComponent, String, Validator...) }.
   * <p>
   * This method creates an {@link SVDecorator}.
   * It assumes that the validator's
   * {@link Validator#validate(org.netbeans.validation.api.Problems,
   * java.lang.String, java.lang.Object) Validator.validate(...)}
   * method does, in addition to SsComponent specific checks, at least
   * {@snippet lang="java" class=SimpleValidation region=validate}
   * 
   * Even though the SsComponent is not a text component,
   * {@link SsComponent#decorateText(dev.visdb.seesaw.utils.SsComponent.ValidationResult)
   * SsComponent.decorateText(result)} should be invoked.
   * If {@code validator} does not invoke decorateText, do something like
   * {@snippet lang="java" class=SimpleValidation region=setDoDecorateText}
   * <p>
   * The caller typically adds the returned ValidationItem to a
   * {@link ValidationGroup}. This can be done individually,
   * but {@link #createDecoratorPanel(Container, ValidationItem...) 
   * createDecorationPanel(uiPanel, ValidationItem...)} is convenient.
   * 
   * @param <T> subclass of SsComponent
   * @param ssComp
   * @param name
   * @param validator
   * @return
   */
  public static <T extends SsComponent> ValidationItem setDecorator(T ssComp, String name,
                                                                    Validator<T> validator) {
    SwingValidationGroup.setComponentName((JComponent)ssComp, name);
    var valItem = new SsCompValidationOnDemand<T>(ssComp, decorationFor(ssComp), validator);
    ssComp.setDecorator(new SVDecorator(valItem));
    return valItem;
  }
  
  /**
   * Find the validation panel containing the component.
   * @param ssComp find an ancestor of this component
   * @return null if not found
   */
  public static ValidationPanel findDecoratorPanel(SsComponent ssComp) {
    return (ValidationPanel) SwingUtilities.getAncestorOfClass(ValidationPanel.class, (Component)ssComp);
  }

  /**
   * Create a decorator panel, a simple JPanel, for use with the SimpleValidation framework;
   * it displays validation problems under the uiPanel.
   * If ValidationItems are present,
   * they are individually added to the ValidationPanel's ValidationGroup.
   * @param uiPanel  app UI, problem descriptions are displayed below it
   * @param validationItems items to add to the {@code ValidationGroup}
   * @return validation panel
   */
  public static ValidationPanel createDecoratorPanel(Container uiPanel, ValidationItem... validationItems) {
    return createDecoratorPanel(uiPanel, false, validationItems);
  }

  /**
   * A convenience method to create a JPanel that displays validation
   * problems associated with the validation items.
   * @param uiPanel  UI which is displayed above the problem label
   * @param disableUI see {@link ValidationGroup#addItem(ValidationItem, boolean) }
   * @param validationItems items to add to the {@link ValidationGroup}
   * @return validation panel
   */
  private static ValidationPanel createDecoratorPanel(
      Container uiPanel, boolean disableUI, ValidationItem... validationItems) {
    ValidationPanel valiPanel = createDecoratorPanel(uiPanel);
    ValidationGroup group = valiPanel.getValidationGroup();
    for(ValidationItem validationItem : validationItems) {
      group.addItem(validationItem, disableUI);
    }
    return valiPanel;
  }

  static ValidationPanel createDecoratorPanel(Container uiPanel) {
    ValidationPanel parentPanel = new ValidationPanel();
    parentPanel.setInnerComponent(uiPanel);
    parentPanel.putClientProperty(Decorator.SEE_SAW_PANEL_KEY, SIMPLE_VALIDATION);
    var adj = Globals.getOption(DecoratorPanelAdjustSOUTH.class);
    if (adj != null) {
      Component p = ((BorderLayout)parentPanel.getLayout()).getLayoutComponent(BorderLayout.SOUTH);
      Dimension d = p.getPreferredSize();
      // HACK
      d.height += adj.d.height;
      d.width += adj.d.width;
      p.setPreferredSize(d);
    }
    return parentPanel;
  }

  /**
   * Get a SimpleValidation UI for {@link SsComponent#getDecorateTarget() }.
   * {@link SsComponent#setDecorateTarget(JComponent) }, if used, should
   * be invoked before {@code decorationFor} is called.
   * @param ssComp
   * @return 
   */
  // TODO: Might want DecorationFactory that takes a Supplier<JComponent>
  //       then can avoid the mess in DefaultValidationDecorator.
  public static ValidationUI decorationFor(SsComponent ssComp) {
    return SwingComponentDecorationFactory.getDefault().decorationFor(ssComp.getDecorateTarget());
  }
}
// vi: sw=2 ts=8
