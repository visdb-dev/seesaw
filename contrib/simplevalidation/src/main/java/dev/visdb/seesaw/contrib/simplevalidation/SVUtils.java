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

import java.awt.Component;
import java.util.function.Function;
import java.util.function.Supplier;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.conversion.Converter;
import org.netbeans.validation.api.ui.ValidationGroup;
import org.netbeans.validation.api.ui.ValidationItem;
import org.netbeans.validation.api.ui.swing.SwingComponentDecorationFactory;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;
import org.netbeans.validation.api.ui.swing.ValidationPanel;

import dev.visdb.seesaw.decorators.Decorator;
import dev.visdb.seesaw.utils.SsComponent;

/**
 * Helpers for working with Simple Validation framework.
 */
public class SVUtils {
  private SVUtils() {}

  /** SimpleValidation decorator style name. */
  public static final Decorator.DecoratorStyle SV_DECORATOR_STYLE
      = new Decorator.DecoratorStyle("SV_DECORATOR_STYLE");

  /**
   * Should probably be from a factory.
   * @param jtc
   * @param validators
   * @return
   */
  @SafeVarargs
  public static TextComponentValidationItem createDefaultTextValidator(
      JTextComponent jtc, Validator<String>... validators) {
    Validator<String> merged = ValidatorUtils.merge(validators);
    Validator<Document> validator = Converter.find(String.class, Document.class).convert(merged);
    TextComponentValidationItem valItem = new TextComponentValidationItem(
        jtc, SwingComponentDecorationFactory.getDefault().decorationFor(jtc), validator);
    return valItem;
  }

  /**
   * Set the decorator for the SsComponent. The pluginValidator equivelent
   * is embedded in StringSsComponentValidator.
   * Use this form when building complex string validators, see 
   * {@link StringSsComponentValidator},
   * {@link org.netbeans.validation.api.Validator }.
   * {@link org.netbeans.validation.api.builtin.stringvalidation.StringValidators} and
   * {@link org.netbeans.validation.api.ValidatorUtils#merge(org.netbeans.validation.api.Validator...) }.
   * @param jtc must be an SsComponent.
   * @param sval
   * @return ValidationItem to assign to the {@link ValidationGroup}.
   */
  private static ValidationItem setDecoratorValidator(JTextComponent jtc, Validator<String> sval) {
    SsComponent comp = (SsComponent) jtc;
    TextComponentValidationItem textVali = createDefaultTextValidator(jtc, sval);
    SimpleValidationDecorator deco = new SimpleValidationDecorator(textVali);
    comp.setDecorator(deco);
    return textVali;
  }

  /**
   * Set decorator for the SsComponent; assign name.
   * Note that JTextComponent.getName() is used, dynamically, if name is null or not set.
   * Create the validator used by the decorator using condition and problemDesc;
   * for more information see
   * {@link StringSsComponentValidator},
   * {@link org.netbeans.validation.api.Validator }.
   * {@link org.netbeans.validation.api.builtin.stringvalidation.StringValidators} and
   * {@link org.netbeans.validation.api.ValidatorUtils#merge(org.netbeans.validation.api.Validator...) }.
   * @param jtc must be an SsComponent.
   * @param name
   * @param condition see {@link StringSsComponentValidator#StringSsComponentValidator(java.util.function.Function, java.util.function.Supplier, dev.visdb.seesaw.utils.SsComponent) StringSsComponentValidator(validationCondition, problemDescription, ssComponent)}
   * @param problemDesc see {@link StringSsComponentValidator#StringSsComponentValidator(java.util.function.Function, java.util.function.Supplier, dev.visdb.seesaw.utils.SsComponent)  StringSsComponentValidator(validationCondition, problemDescription, ssComponent)}
   * @return ValidationItem to assign to the {@link ValidationGroup}.
   */
  public static ValidationItem setDecoratorValidator(JTextComponent jtc, String name,
                                                     Function<String, Boolean> condition,
                                                     Supplier<String> problemDesc) {
    SwingValidationGroup.setComponentName(jtc, name);
    StringSsComponentValidator sval = new StringSsComponentValidator(condition, problemDesc, (SsComponent)jtc);
    return setDecoratorValidator(jtc, sval);
  }


  /**
   * A convenience method to create a JPanel that displays validation
   * problems associated with the validation items.
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
  public static ValidationPanel createDecoratorPanel(Component c, boolean disableUI,
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
    return parentPanel;
  }
}
// vi: sw=2 ts=8
