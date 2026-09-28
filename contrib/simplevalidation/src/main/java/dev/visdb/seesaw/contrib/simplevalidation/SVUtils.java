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
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.builtin.stringvalidation.StringValidators;
import org.netbeans.validation.api.conversion.Converter;
import org.netbeans.validation.api.ui.ValidationGroup;
import org.netbeans.validation.api.ui.ValidationItem;
import org.netbeans.validation.api.ui.ValidationListener;
import org.netbeans.validation.api.ui.ValidationUI;
import org.netbeans.validation.api.ui.swing.SwingComponentDecorationFactory;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.netbeans.validation.api.ui.swing.ValidationPanel;

import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.utils.Globals;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsUtils.DecoratorPanelAdjustSOUTH;

/**
 * Static methods for working with Simple Validation framework.
 */
public class SVUtils {
  private SVUtils() {}

  /** SimpleValidation decorator style name. */
  public static final Decorator.DecoratorStyle SV_DECORATOR_STYLE
      = new Decorator.DecoratorStyle("SV_DECORATOR_STYLE");

  /**
   * Create an on demand {@link ValidationListener} for an {@link SsComponent};
   * Exception if not a {@code JTextComponent}.
   * It does no listening since its invocation is handled by SeeSaw.
   * @param ssComp
   * @param validators
   * @return
   */
  @SafeVarargs
  public static JTextComponentValidationOnDemand createTextValidationOnDemand(
      SsComponent ssComp, Validator<String>... validators) {
    Validator<String> merged = ValidatorUtils.merge(validators);
    Validator<Document> validator = Converter.find(String.class, Document.class).convert(merged);
    JTextComponentValidationOnDemand valItem = new JTextComponentValidationOnDemand(
        (JTextComponent)ssComp, decorationFor(ssComp), validator);
    return valItem;
  }

  /**
   * Create a ValidationItem from the merged {@code validators}, {@code validationCondition}
   * and {@code problemDescription};
   * create a {@link Decorator} using the new ValidationItem; and set the decorator on the SsComponent.
   * The ssComponent must be a {@code JTextComponent}.
   * For more information see
   * {@link #createTextValidationOnDemand(SsComponent, Validator...) },
   * {@link Validator }, {@link StringValidators} and {@link ValidatorUtils#merge(Validator...) }.
   * The {@code name} is assigned to the SsComponent.
   * Note that JTextComponent.getName() is used, dynamically, if {@code name} is null
   * or not otherwise set.
   * <p>
   * The returned ValidationItem is typically added to a {@link ValidationGroup}.
   * This can be done manually,
   * but {@link #createDecoratorPanel(Container, ValidationItem...) 
   * createDecorationPanel(uiPanel, ValidationItem...}
   * is convenient.
   * <p>
   * <b> If {@link SsComponent#isComposite() ssComp.isComposite()}
   * is true</b>, then
   * {@link SsComponent#setDecorateTarget(JComponent)}
   * should be called before setDecoratorValidator is called.
   * This is typically not an issue.
   * @param ssComp must be a JTextComponent
   * @param name available for problem messages
   * @param validationCondition see {@link StringSsComponentValidator#StringSsComponentValidator(Function, Supplier, SsComponent)}
   * @param problemDescription see {@link StringSsComponentValidator#StringSsComponentValidator(Function, Supplier, SsComponent)}
   * @param validators
   * @return ValidationItem to assign to the {@link ValidationGroup}.
   */
  // TODO: Might want DecorationFactory that takes a Supplier<JComponent>
  //       then can avoid the mess in DefaultValidationDecorator.
  @SafeVarargs
  public static ValidationItem setDecoratorValidator(SsComponent ssComp, String name,
                                                     Function<String, Boolean> validationCondition,
                                                     Supplier<String> problemDescription,
                                                     Validator<String>... validators) {
    JTextComponent jtc = (JTextComponent)ssComp; // fast fail if wrong type
    SwingValidationGroup.setComponentName(jtc, name);
    StringSsComponentValidator stringVali = new StringSsComponentValidator(
        validationCondition, problemDescription, ssComp);
    Validator<String>[] newValidatorArray = Arrays.copyOf(validators, validators.length + 1);
    newValidatorArray[newValidatorArray.length - 1] = stringVali;
    return setDecoratorValidator(ssComp, newValidatorArray);
  }

  @SafeVarargs
  private static ValidationItem setDecoratorValidator(SsComponent ssComp,
                                                      Validator<String>... validators) {
    JTextComponentValidationOnDemand textVali = createTextValidationOnDemand(ssComp, validators);
    SimpleValidationDecorator deco = new SimpleValidationDecorator(textVali);
    ssComp.setDecorator(deco);
    return textVali;
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
   * @param disableUI see {@link ValidationGroup#addItem(org.netbeans.validation.api.ui.ValidationItem, boolean) }
   * @param validationItems items to add to the ValidationGroup
   * @return validation panel
   */
  static ValidationPanel createDecoratorPanel(Container uiPanel, boolean disableUI,
                                             ValidationItem... validationItems) {
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
    parentPanel.putClientProperty(Decorator.SEE_SAW_PANEL_KEY, SV_DECORATOR_STYLE);
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
