/*
 * Copyright 2010-2019 Tim Boudreau
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package dev.visdb.seesaw.contrib.simplevalidation;

import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import javax.swing.text.JTextComponent;

import org.netbeans.validation.api.Problems;
import org.netbeans.validation.api.Validator;
import org.netbeans.validation.api.ValidatorUtils;
import org.netbeans.validation.api.conversion.Converter;
import org.netbeans.validation.api.ui.*;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;

import dev.visdb.seesaw.utils.SsComponent;

import static dev.visdb.seesaw.contrib.simplevalidation.SVUtils.decorationFor;

/**
 * Hook into SimpleValidation framework; validate on demand, not a listener,
 * see {@link SVUtils#setTextDecorator(dev.visdb.seesaw.utils.SsComponent,
 * java.lang.String, org.netbeans.validation.api.Validator...)
 * SVUtils.setTextDecorator(ssComp, name, validator...)}
 * SeeSaw decorator invokes this as needed.
 */
// Derived from validation.api.ui.JTextComponentValidationListenerImpl 
// Copied from:
//      validation/api/ui/JTextComponentValidationListenerImpl.java
// TODO: Make this independent of Document, just use a string?
//		 Set the string on every change to text, after change needs validation?
public class TextValidationOnDemand extends ValidationListener<JTextComponent> {
  private final Validator<Document> validator;

  /**
   * Create an on demand {@link ValidationListener} for an {@link SsComponent},
   * exception if not a {@code JTextComponent}.
   * On demand, does no listening, invocation is handled by a SeeSaw decorator.
   * @param ssComp
   * @param validators
   * @return
   */
  @SafeVarargs
  public static TextValidationOnDemand create(
      SsComponent ssComp, Validator<String>... validators) {
    Validator<String> merged = ValidatorUtils.merge(validators);
    Validator<Document> validator = Converter.find(String.class, Document.class).convert(merged);
    return new TextValidationOnDemand((JTextComponent)ssComp, decorationFor(ssComp), validator);
  }

  /**
   * Create ValidationItem, used only on demand, for the specified component.
   * @param component
   * @param validationUI
   * @param validator 
   */
  public TextValidationOnDemand(JTextComponent component, ValidationUI validationUI,
                                     Validator<Document> validator) {
    super(JTextComponent.class, validationUI, component);
    this.validator = validator;
  }

  /**
   * Run validation on the SsComponent.
   * @param ps
   */
  @Override
  protected final void performValidation(Problems ps) {
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> performValidation(ps));
    }
    JTextComponent component = getTarget();
    if (!component.isEnabled()) {
      return;
    }
    validator.validate(ps, SwingValidationGroup.nameForComponent(component),
                       component.getDocument());
  }
}
// vi: sw=2 ts=8
