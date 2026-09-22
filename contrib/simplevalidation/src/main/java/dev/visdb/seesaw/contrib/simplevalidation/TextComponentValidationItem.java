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
 * SeeSaw invokes as needed.
 * Derived from validation.api.ui.JTextComponentValidationListenerImpl 
 */
// TODO: Make this independent of Document, just use a string?
//		 Set the string on every change to text, after change needs validation?
//public class TextComponentValidationItem extends ValidationListener<JTextComponent>
//implements DocumentListener, FocusListener, Runnable
public class TextComponentValidationItem extends ValidationListener<JTextComponent> {
  private Validator<Document> validator;

  /**
   * Create ValidationItem for on demand only for the specified component.
   * @param component
   * @param strategy
   * @param validationUI
   * @param validator 
   */
  public TextComponentValidationItem(JTextComponent component, ValidationStrategy strategy,
                                     ValidationUI validationUI, Validator<Document> validator) {
    super(JTextComponent.class, validationUI, component);
    this.validator = validator;
    if (strategy == null) {
      throw new NullPointerException("strategy null");
    }
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

/* WAS AT END OF Contructor, just before performValidaiton.
        component.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                performValidation();
            }
        });
        switch (strategy) {
            case DEFAULT:
            case ON_CHANGE_OR_ACTION:
                component.getDocument().addDocumentListener(this);
                break;
            case INPUT_VERIFIER:
                component.setInputVerifier( new InputVerifier() {
                    @Override
                    public boolean verify(JComponent input) {
                                                performValidation();
                                                return !hasFatalProblem;
                    }
                });
                break;
            case ON_FOCUS_LOSS:
                component.addFocusListener(this);
                break;
        }
*/
/*
    @Override
    public void focusLost(FocusEvent e) {
        performValidation();
    }

    @Override
    public void focusGained(FocusEvent e) {
    }

    @Override
    public void insertUpdate(DocumentEvent e) {
        removeUpdate(e);
    }

    @Override
    public void removeUpdate(DocumentEvent e) {
        //Documents can be legally updated from another thread,
        //but we will not run validation outside the EDT
        if (!EventQueue.isDispatchThread()) {
            EventQueue.invokeLater(this);
        } else {
            performValidation();
        }
    }

    @Override
    public void changedUpdate(DocumentEvent e) {
        removeUpdate(e);
    }

    // See removeUpdate..
    @Override
    public void run() {
        performValidation();
    }
*/

// vi: sw=2 ts=8
