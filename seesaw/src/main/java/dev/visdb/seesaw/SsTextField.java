/*******************************************************************************
 * Copyright (C) 2003-2021, Prasanth R. Pasala, Brian E. Pangburn, & The Pangburn Group
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
 ******************************************************************************/
/* *****************************************************************************
 * The conditions in the above copyright notice apply to this copyright notice.
 * Additions and modifications made by Ernie R. Rael are
 * copyright (C) 2024-2026, Ernie R. Rael. All rights reserved.
 * ****************************************************************************/
package dev.visdb.seesaw;

import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.lang.System.Logger;
import java.util.EventListener;

import javax.swing.JTextField;

import dev.visdb.seesaw.navigate.RowsModel;
import dev.visdb.seesaw.utils.JStuff;
import dev.visdb.seesaw.utils.SsComponent;
import dev.visdb.seesaw.utils.SsTextSupport;
import dev.visdb.seesaw.utils.SsTextSupport.SsDocumentListener;
import dev.visdb.seesaw.utils.SsUtils;

import static dev.visdb.seesaw.utils.JStuff.sf;
import static java.lang.System.Logger.Level.*;

/**
 * SsTextField extends the JTextField.
 */
@SuppressWarnings("serial")
public class SsTextField extends JTextField implements SsComponent {
  // TODO Consider adding an InputVerifier to prevent component from
  // losing focus; see FormattedTextField.

  /** Logger for component */
  private static final Logger logger = JStuff.getLogger();

  /**
   * Constructs a new, empty text field.
   */
  public SsTextField() {
    this(null);
  }

  /**
   * Constructs a new text field with the given text.
   * @param text initial text
   */
  public SsTextField(String text) {
    this(text, null, null);
  }

  /**
   * Creates a TextField instance and binds it to the specified RowSet column.
   *
   * @param rowsModel        model for a RowSet
   * @param columnName name of the column to which this label should be bound
   */
  public SsTextField(RowsModel rowsModel, String columnName) {
    this(null, rowsModel, columnName);
  }

  /** All the constructors feed through here */
  private SsTextField(String text, RowsModel rowsModel, String columnName) {
    super(text);
    finishSsCommon();
    if (rowsModel != null)
      rowsModel.bind(this, columnName);
  }

  // /**
  //  * Part of the scheme to keep text field in sync with data base.
  //  */
  // @Override
  // protected Document createDefaultModel() {
  // 	Document doc = super.createDefaultModel();
  // 	return doc;
  // 	// return new SSPlainDocument(this);
  // }

  /**
   * Add focus listener that selects all text.
   * Add key listener for when this is used with mask. Use Mask Formatters.
   */
  @Override
  public void customInit() {
    // ADD FOCUS LISTENER TO THE TEXT FIELD SO THAT WHEN THE FOCUS IS GAINED
    // COMPLETE TEXT SHOULD BE SELECTED
    addFocusListener(new FocusAdapter() {
      @Override
      public void focusGained(FocusEvent fe) {
        // TODO: Turn off any TextDecorator while focused
        SsTextField.this.selectAll();
      }
    });
  }

  /** {@inheritDoc } */
  @Override
  public void cleanField() {
    setText("");
  }

  private Hook hook;

  /** {@inheritDoc } */
  @Override
  public final Hook getSsComponentHook() {
    if (hook == null)
      hook = new Hook(this) {
        /**
         * Updates the value stored and displayed in the SwingSet
         * component based on getColumnText()
         */
        @Override
        protected void updateSsComponent() {
          final String text = getColumnText();
          logger.log(DEBUG, () -> sf("%s: Setting text field to %s.", getColumnForLog(), text));
          setText(text);
        }

        /** {@inheritDoc } */
        @Override
        protected SsDocumentListener getSsComponentListener() {
          return SsTextSupport.getSsDocumentListener(SsTextField.this);
        }

        /** {@inheritDoc } */
        @Override
        protected void addSsComponentListener(EventListener eventListener) {
          getDocument().addDocumentListener((SsDocumentListener) eventListener);
        }

        /** {@inheritDoc } */
        @Override
        protected void removeSsComponentListener(EventListener eventListener) {
          getDocument().removeDocumentListener((SsDocumentListener) eventListener);
        }
      };
    return hook;
  }

  /** {@inheritDoc} */
  @Override
  public String toString() {
    return sf("%s{text=%s, %s}", getClass().getSimpleName(), getText(),
              SsUtils.ssComponentToString(this));
  }

} // end public class SsTextField extends JTextField {
