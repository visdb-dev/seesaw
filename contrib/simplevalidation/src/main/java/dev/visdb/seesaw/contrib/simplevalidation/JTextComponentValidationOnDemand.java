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
import org.netbeans.validation.api.ui.*;
import org.netbeans.validation.api.ui.swing.SwingValidationGroup;

/**
 * Hook into SimpleValidation framework; validate on demand, not as a listener.
 * That is the key difference from a typical validation listener,
 * this does not add itself as a listener.
 * SeeSaw decorator invokes this as needed.
 * Derived from validation.api.ui.JTextComponentValidationListenerImpl 
 */
// Copied from:
//      validation/api/ui/JTextComponentValidationListenerImpl.java
// TODO: Make this independent of Document, just use a string?
//		 Set the string on every change to text, after change needs validation?
public class JTextComponentValidationOnDemand extends ValidationListener<JTextComponent> {
  private final Validator<Document> validator;

  /**
   * Create ValidationItem for on demand only for the specified component.
   * @param component
   * @param validationUI
   * @param validator 
   */
  public JTextComponentValidationOnDemand(JTextComponent component, ValidationUI validationUI,
                                     Validator<Document> validator) {
    super(JTextComponent.class, validationUI, component);
    this.validator = validator;
  }

  /**
   * Throw an IllegalStateException if component does not match the target.
   * @param component
   */
  public void verifyComponent(Object component) {
    if (component != getTarget())
      throw new IllegalStateException("decorating the wrong component");
  }
  /**
   * @return The JTextComponent that this ValidationItem is hooked to.
   */
  public JTextComponent getComponent() {
    return getTarget();
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
