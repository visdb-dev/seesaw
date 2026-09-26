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
import dev.visdb.seesaw.utils.CentralLookup;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsUtils.DecoratorPanelAdjustSOUTH;

/**
 * Helpers for working with Simple Validation framework.
 */
public class SVUtils {
  private SVUtils() {}

  /** SimpleValidation decorator style name. */
  public static final Decorator.DecoratorStyle SV_DECORATOR_STYLE
      = new Decorator.DecoratorStyle("SV_DECORATOR_STYLE");

  /**
   * Create an on demand {@link ValidationListener} for a {@code JTextComponent};
   * it does no listening.
   * @param jtc
   * @param validators
   * @return
   */
  @SafeVarargs
  public static JTextComponentValidationOnDemand createTextValidationOnDemand(
      JTextComponent jtc, Validator<String>... validators) {
    Validator<String> merged = ValidatorUtils.merge(validators);
    Validator<Document> validator = Converter.find(String.class, Document.class).convert(merged);
    JTextComponentValidationOnDemand valItem = new JTextComponentValidationOnDemand(
        jtc, decorationFor((SsComponent)jtc), validator);
    return valItem;
  }

  @SafeVarargs
  private static ValidationItem setDecoratorValidator(JTextComponent jtc,
                                                      Validator<String>... validators) {
    SsComponent comp = (SsComponent) jtc;
    JTextComponentValidationOnDemand textVali = createTextValidationOnDemand(jtc, validators);
    SimpleValidationDecorator deco = new SimpleValidationDecorator(textVali);
    comp.setDecorator(deco);
    return textVali;
  }

  /**
   * Set decorator on the text SsComponent; assign name.
   * Create a validator, {@link StringSsComponentValidator},
   * using validationCondition and problemDescription. Merge this validator with the param validators
   * to create the validation,
   * {@link #createTextValidationOnDemand(JTextComponent, Validator...) }
   * used by the decorator.
   * For more information see
   * {@link Validator }, {@link StringValidators} and {@link ValidatorUtils#merge(Validator...) }.
   * Note that JTextComponent.getName() is used, dynamically, if name is null
   * or not otherwise set.
   * <p>
   * <b> If SsComponent.setDecoratorTarget is used then this should be called after.</b>
   * @param jtc must be an SsComponent.
   * @param name
   * @param validationCondition see {@link StringSsComponentValidator#StringSsComponentValidator(Function, Supplier, SsComponent)}
   * @param problemDescription see {@link StringSsComponentValidator#StringSsComponentValidator(Function, Supplier, SsComponent)}
   * @param validators
   * @return ValidationItem to assign to the {@link ValidationGroup}.
   */
  // TODO: Might want DecorationFactory that takes a Supplier<JComponent>
  //       then can avoid the mess in DefaultValidationDecorator.
  @SafeVarargs
  public static ValidationItem setDecoratorValidator(JTextComponent jtc, String name,
                                                     Function<String, Boolean> validationCondition,
                                                     Supplier<String> problemDescription,
                                                     Validator<String>... validators) {
    SwingValidationGroup.setComponentName(jtc, name);
    StringSsComponentValidator stringVali = new StringSsComponentValidator(validationCondition,
                                                                           problemDescription,
                                                                           (SsComponent)jtc);
    Validator<String>[] newValidatorArray = Arrays.copyOf(validators, validators.length + 1);
    newValidatorArray[newValidatorArray.length - 1] = stringVali;
    return setDecoratorValidator(jtc, newValidatorArray);
  }


  /**
   * A convenience method to create a JPanel that displays validation
   * problems associated with the validation items. The validation items
   * are added to the ValidationPanel's validation group.
   * The disableUI is false.
   * @param c  UI which is displayed above the problem label
   * @param validationItems items to add to the {@code ValidationGroup}
   * @return validation panel
   */
  public static ValidationPanel createDecoratorPanel(Component c, ValidationItem... validationItems) {
    return createDecoratorPanel(c, false, validationItems);
  }

  /**
   * A convenience method to create a JPanel that displays validation
   * problems associated with the validation items.
   * @param c  UI which is displayed above the problem label
   * @param disableUI see {@link ValidationGroup#addItem(org.netbeans.validation.api.ui.ValidationItem, boolean) }
   * @param validationItems items to add to the ValidationGroup
   * @return validation panel
   */
  static ValidationPanel createDecoratorPanel(Component c, boolean disableUI,
                                             ValidationItem... validationItems) {
    ValidationPanel valiPanel = createDecoratorPanel(c);
    ValidationGroup group = valiPanel.getValidationGroup();
    for(ValidationItem validationItem : validationItems) {
      group.addItem(validationItem, disableUI);
    }
    return valiPanel;
  }

  /**
   * Find the validation panel containing the component.
   * @param jc find an ancestor of this component
   * @return null if not found
   */
  public static ValidationPanel findDecoratorPanel(JComponent jc) {
    return (ValidationPanel) SwingUtilities.getAncestorOfClass(ValidationPanel.class, jc);
  }

  static ValidationPanel createDecoratorPanel(Component uiPanel) {
    ValidationPanel parentPanel = new ValidationPanel();
    parentPanel.setInnerComponent(uiPanel);
    parentPanel.putClientProperty(Decorator.SEE_SAW_PANEL_KEY, SV_DECORATOR_STYLE);
    var adj = CentralLookup.defLookup(DecoratorPanelAdjustSOUTH.class);
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
   * An SsComponent may decorate and alternate
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
